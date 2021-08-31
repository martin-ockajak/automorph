package automorph.client

import automorph.log.MacroLogger
import automorph.spi.MessageCodec
import automorph.spi.protocol.RpcFunction
import automorph.util.{MethodReflection, Reflection}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** RPC client layer bindings code generation. */
case object ClientGenerator {

  /**
   * Generates client bindings for all valid public methods of an API type.
   *
   * @param codec message codec plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam Api API type
   * @return mapping of API method names to client function bindings
   */
  def bindings[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](
    codec: Codec
  ): Map[String, ClientBinding[Node]] = macro bindingsMacro[Node, Codec, Effect, Context, Api]

  def bindingsMacro[
    Node: c.WeakTypeTag,
    Codec <: MessageCodec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(codec: c.Expr[Codec])(implicit
    effectType: c.WeakTypeTag[Effect[_]]
  ): c.Expr[Map[String, ClientBinding[Node]]] = {
    import c.universe.Quasiquote
    val ref = Reflection[c.type](c)

    // Detect and validate public methods in the API type
    val apiMethods = MethodReflection.apiMethods[c.type, Api, Effect[_]](ref)
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
      q"${method.name} -> ${generateBinding[c.type, Node, Codec, Effect, Context, Api](ref)(method, codec)}"
    }
    c.Expr[Map[String, ClientBinding[Node]]](q"""
      Seq(..$clientMethods).toMap
    """)
  }

  private def generateBinding[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Codec <: MessageCodec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api: ref.c.WeakTypeTag
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    codec: ref.c.Expr[Codec]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[ClientBinding[Node]] = {
    import ref.c.universe.{Liftable, Quasiquote, weakTypeOf}

    val nodeType = weakTypeOf[Node]
    val encodeArguments = generateEncodeArguments[C, Node, Codec, Context](ref)(method, codec)
    val decodeResult = generateDecodeResult[C, Node, Codec, Effect](ref)(method, codec)
    logBoundMethod[C, Api](ref)(method, encodeArguments, decodeResult)
    implicit val functionLiftable: Liftable[RpcFunction] = MethodReflection.functionLiftable(ref)
    Seq(functionLiftable)
    ref.c.Expr[ClientBinding[Node]](q"""
      automorph.client.ClientBinding[$nodeType](
        ${method.lift.rpcFunction},
        $encodeArguments,
        $decodeResult,
        ${MethodReflection.usesContext[C, Context](ref)(method)}
      )
    """)
  }

  private def generateEncodeArguments[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    Codec <: MessageCodec[Node]: ref.c.WeakTypeTag,
    Context: ref.c.WeakTypeTag
  ](ref: Reflection[C])(method: ref.RefMethod, codec: ref.c.Expr[Codec]): ref.c.Expr[Seq[Any] => Seq[Node]] = {
    import ref.c.universe.{Quasiquote, weakTypeOf}
    (weakTypeOf[Node], weakTypeOf[Codec])

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
          Option.when((offset + index) != lastArgumentIndex || !MethodReflection.usesContext[C, Context](ref)(method)) {
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
    Codec <: MessageCodec[Node]: ref.c.WeakTypeTag,
    Effect[_]
  ](ref: Reflection[C])(method: ref.RefMethod, codec: ref.c.Expr[Codec])(implicit
    effectType: ref.c.WeakTypeTag[Effect[_]]
  ): ref.c.Expr[Node => Any] = {
    import ref.c.universe.{Quasiquote, weakTypeOf}
    (weakTypeOf[Node], weakTypeOf[Codec])

    // Create decode result function
    //   (resultNode: Node) => ResultValueType = codec.dencode[ResultValueType](resultNode)
    val nodeType = weakTypeOf[Node]
    val resultValueType = MethodReflection.unwrapType[C, Effect[_]](ref.c)(method.resultType).dealias
    ref.c.Expr[Node => Any](q"""
      (resultNode: $nodeType) => $codec.decode[$resultValueType](resultNode)
    """)
  }

  private def logBoundMethod[C <: blackbox.Context, Api: ref.c.WeakTypeTag](ref: Reflection[C])(
    method: ref.RefMethod,
    encodeArguments: ref.c.Expr[Any],
    decodeResult: ref.c.Expr[Any]
  ): Unit = MacroLogger.debug(
    s"""${MethodReflection.signature[C, Api](ref)(method)} =
      |  ${ref.c.universe.showCode(encodeArguments.tree)}
      |  ${ref.c.universe.showCode(decodeResult.tree)}
      |""".stripMargin
  )
}
