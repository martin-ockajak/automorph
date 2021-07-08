package automorph.client

import automorph.client.ClientBinding
import automorph.protocol.MethodBindings.{methodSignature, methodLiftable, methodUsesContext, unwrapType, validApiMethods}
import automorph.spi.Codec
import automorph.util.Reflection
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** JSON-RPC client layer bindings code generation. */
case object ClientBindings {

  private val debugProperty = "automorph.macro.debug"

  /**
   * Generates client bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @tparam Node message node type
   * @tparam ExactCodec message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam Api API type
   * @return mapping of method names to client method bindings
   */
  def generate[Node, ExactCodec <: Codec[Node], Effect[_], Context, Api <: AnyRef](
    codec: ExactCodec
  ): Map[String, ClientBinding[Node]] = macro generateMacro[Node, ExactCodec, Effect, Context, Api]

  def generateMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(codec: c.Expr[ExactCodec])(implicit
    effectType: c.WeakTypeTag[Effect[_]]
  ): c.Expr[Map[String, ClientBinding[Node]]] = {
    import c.universe.Quasiquote
    val ref = Reflection[c.type](c)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[c.type, Api, Effect[_]](ref)
    val validMethods = apiMethods.flatMap(_.swap.toOption) match {
      case Seq() => apiMethods.flatMap(_.toOption)
      case errors =>
        ref.c.abort(
          ref.c.enclosingPosition,
          s"Failed to bind API methods:\n${errors.map(error => s"  $error").mkString("\n")}"
        )
    }

    // Generate bound API method bindings
    val clientMethods = validMethods.map { method =>
      q"${method.name} -> ${generateBinding[c.type, Node, ExactCodec, Effect, Context, Api](ref)(method, codec)}"
    }
    c.Expr[Map[String, ClientBinding[Node]]](q"""
      Seq(..$clientMethods).toMap
    """)
  }

  private def generateBinding[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    ExactCodec <: Codec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api: ref.c.WeakTypeTag
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    codec: ref.c.Expr[ExactCodec]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[ClientBinding[Node]] = {
    import ref.c.universe.Quasiquote

    val encodeArguments = generateEncodeArguments[C, Node, ExactCodec, Context](ref)(method, codec)
    val decodeResult = generateDecodeResult[C, Node, ExactCodec, Effect](ref)(method, codec)
    logBoundMethod[C, Api](ref)(method, encodeArguments, decodeResult)
    implicit val methodLift = methodLiftable(ref)
    Seq(methodLift)
    ref.c.Expr[ClientBinding[Node]](q"""
      automorph.client.ClientBinding(
        ${method.lift},
        $encodeArguments,
        $decodeResult,
        ${methodUsesContext[C, Context](ref)(method)}
      )
    """)
  }

  private def generateEncodeArguments[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    ExactCodec <: Codec[Node]: ref.c.WeakTypeTag,
    Context: ref.c.WeakTypeTag
  ](ref: Reflection[C])(method: ref.RefMethod, codec: ref.c.Expr[ExactCodec]): ref.c.Expr[Seq[Any] => Seq[Node]] = {
    import ref.c.universe.{weakTypeOf, Quasiquote}
    (weakTypeOf[Node], weakTypeOf[ExactCodec])

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create encode arguments function
    //   (arguments: Seq[Any]) => Seq[Node]
    ref.c.Expr[Seq[Any] => Seq[Node]](q"""
      (arguments: Seq[Any]) => ${
      // Create the method argument lists by encoding corresponding argument values into nodes
      //   List(
      //     codec.encode[Parameter0Type](arguments(0).asInstanceOf[Parameter0Type]),
      //     ...
      //     codec.encode[ParameterNType](arguments(N).asInstanceOf[ParameterNType])
      //   ): List[Node]
      val argumentNodes = method.parameters.toList.zip(parameterListOffsets).flatMap { case (parameters, offset) =>
        parameters.toList.zipWithIndex.flatMap { case (parameter, index) =>
          Option.when((offset + index) != lastArgumentIndex || !methodUsesContext[C, Context](ref)(method)) {
            q"$codec.encode[${parameter.dataType}](arguments(${offset + index}).asInstanceOf[${parameter.dataType}])"
          }
        }
      }

      // Create the encoded arguments sequence construction call
      //   Seq(encodedArguments*): Seq[Node]
      q"Seq(..$argumentNodes)"
    }
    """)
  }

  private def generateDecodeResult[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    ExactCodec <: Codec[Node]: ref.c.WeakTypeTag,
    Effect[_]
  ](ref: Reflection[C])(method: ref.RefMethod, codec: ref.c.Expr[ExactCodec])(implicit
    effectType: ref.c.WeakTypeTag[Effect[_]]
  ): ref.c.Expr[Node => Any] = {
    import ref.c.universe.{weakTypeOf, Quasiquote}
    (weakTypeOf[Node], weakTypeOf[ExactCodec])

    // Create decode result function
    //   (resultNode: Node) => ResultValueType = codec.dencode[ResultValueType](resultNode)
    val nodeType = weakTypeOf[Node].dealias
    val resultValueType = unwrapType[C, Effect[_]](ref)(method.resultType.dealias).dealias
    ref.c.Expr[Node => Any](q"""
      (resultNode: $nodeType) => $codec.decode[$resultValueType](resultNode)
    """)
  }

  private def logBoundMethod[C <: blackbox.Context, Api: ref.c.WeakTypeTag](ref: Reflection[C])(
    method: ref.RefMethod,
    encodeArguments: ref.c.Expr[Any],
    decodeResult: ref.c.Expr[Any]
  ): Unit = Option(System.getProperty(debugProperty)).orElse(Some("")).foreach { _ =>
    println(
      s"""${methodSignature[C, Api](ref)(method)} =
        |  ${ref.c.universe.showCode(encodeArguments.tree)}
        |  ${ref.c.universe.showCode(decodeResult.tree)}
        |""".stripMargin
    )
  }
}
