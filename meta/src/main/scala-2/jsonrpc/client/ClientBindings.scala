package jsonrpc.client

import jsonrpc.client.ClientMethod
import jsonrpc.protocol.MethodBindings.{methodSignature, methodUsesContext, unwrapType, validApiMethods}
import jsonrpc.spi.Codec
import jsonrpc.util.Reflection
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** JSON-RPC client layer bindings code generation. */
private[jsonrpc] case object ClientBindings {

  private val debugProperty = "jsonrpc.macro.debug"

  /**
   * Generate client bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to client method bindings
   */
  def generate[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType
  ): Map[String, ClientMethod[Node]] = macro generateExpr[Node, CodecType, Effect, Context, ApiType]

  def generateExpr[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    ApiType <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(codec: c.Expr[CodecType])(implicit
    effectType: c.WeakTypeTag[Effect[_]]
  ): c.Expr[Map[String, ClientMethod[Node]]] = {
    import c.universe.Quasiquote
    val ref = Reflection[c.type](c)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[c.type, ApiType, Effect[_]](ref)
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
      q"${method.name} -> ${generateClientMethod[c.type, Node, CodecType, Effect, Context, ApiType](ref)(method, codec)}"
    }
    c.Expr[Map[String, ClientMethod[Node]]](q"""
      Seq(..$clientMethods).toMap
    """)
  }

  private def generateClientMethod[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    CodecType <: Codec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    ApiType: ref.c.WeakTypeTag
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    codec: ref.c.Expr[CodecType]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[ClientMethod[Node]] = {
    import ref.c.universe.Quasiquote

    val encodeArguments = generateEncodeArguments[C, Node, CodecType, Context](ref)(method, codec)
    val decodeResult = generateDecodeResult[C, Node, CodecType, Effect](ref)(method, codec)
    logBoundMethod[C, ApiType](ref)(method, encodeArguments, decodeResult)
    ref.c.Expr[ClientMethod[Node]](q"""
      ClientMethod(
        $encodeArguments,
        $decodeResult,
        ${method.lift.name},
        ${method.lift.resultType},
        ..${method.lift.parameters.flatMap(_.map(_.name))},
        ..${method.lift.parameters.flatMap(_.map(_.dataType))},
        ${methodUsesContext[C, Context](ref)(method)}
      )
    """)
  }

  private def generateEncodeArguments[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    CodecType <: Codec[Node]: ref.c.WeakTypeTag,
    Context: ref.c.WeakTypeTag
  ](ref: Reflection[C])(method: ref.RefMethod, codec: ref.c.Expr[CodecType]): ref.c.Expr[Seq[Any] => Seq[Node]] = {
    import ref.c.universe.{weakTypeOf, Quasiquote}
    (weakTypeOf[Node], weakTypeOf[CodecType])

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
      //     codec.encode[Parameter1Type](arguments(1).asInstanceOf[Parameter1Type]),
      //     ...
      //     codec.encode[ParameterNType](arguments(N).asInstanceOf[ParameterNType])
      //   ): List[Node]
      val argumentNodes = method.parameters.toList.zip(parameterListOffsets).flatMap { case (parameters, offset) =>
        parameters.toList.zipWithIndex.flatMap { case (parameter, index) =>
          Option.when((offset + index) != lastArgumentIndex || !methodUsesContext[C, Context](ref)(method)) {
            q"$codec.encode(arguments[${parameter.dataType}](${offset + index}))"
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
    CodecType <: Codec[Node]: ref.c.WeakTypeTag,
    Effect[_]
  ](ref: Reflection[C])(method: ref.RefMethod, codec: ref.c.Expr[CodecType])(implicit
    effectType: ref.c.WeakTypeTag[Effect[_]]
  ): ref.c.Expr[Node => Any] = {
    import ref.c.universe.{weakTypeOf, Quasiquote}
    (weakTypeOf[Node], weakTypeOf[CodecType])

    // Create decode result function
    //   (resultNode: Node) => ResultValueType = codec.dencode[ResultValueType](resultNode)
    val nodeType = weakTypeOf[Node]
    val resultValueType = unwrapType[C, Effect[_]](ref)(method.resultType)
    ref.c.Expr[Node => Any](q"""
      (resultNode: $nodeType) => $codec.decode[$resultValueType](resultNode)
    """)
  }

  private def logBoundMethod[C <: blackbox.Context, ApiType: ref.c.WeakTypeTag](ref: Reflection[C])(
    method: ref.RefMethod,
    encodeArguments: ref.c.Expr[Any],
    decodeResult: ref.c.Expr[Any]
  ): Unit = if (Option(System.getProperty(debugProperty)).nonEmpty) {
    println(
      s"""${methodSignature[C, ApiType](ref)(method)} =
        |  ${ref.c.universe.showCode(encodeArguments.tree)}
        |  ${ref.c.universe.showCode(decodeResult.tree)}
        |""".stripMargin
    )
  }
}
