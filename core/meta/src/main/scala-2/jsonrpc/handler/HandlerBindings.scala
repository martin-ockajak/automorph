package jsonrpc.handler

import jsonrpc.protocol.MethodBindings.{methodSignature, methodUsesContext, unwrapType, validApiMethods}
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.util.Reflection
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** JSON-RPC handler layer bindings code generation. */
case object HandlerBindings {

  private val debugProperty = "jsonrpc.macro.debug"

  /**
   * Generate handler bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @param backend effect backend plugin
   * @param api API instance
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam Api API type
   * @return mapping of method names to handler method bindings
   */
  def bind[Node, CodecType <: Codec[Node], Effect[_], Context, Api <: AnyRef](
    codec: CodecType,
    backend: Backend[Effect],
    api: Api
  ): Map[String, HandlerMethod[Node, Effect, Context]] = macro bindMacro[Node, CodecType, Effect, Context, Api]

  def bindMacro[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    Api <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    codec: c.Expr[CodecType],
    backend: c.Expr[Backend[Effect]],
    api: c.Expr[Api]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Map[String, HandlerMethod[Node, Effect, Context]]] = {
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
    val handlerMethods = validMethods.map { method =>
      q"${method.name} -> ${generateHandlerMethod[c.type, Node, CodecType, Effect, Context, Api](ref)(method, codec, backend, api)}"
    }
    c.Expr[Map[String, HandlerMethod[Node, Effect, Context]]](q"""
      Seq(..$handlerMethods).toMap
    """)
  }

  private def generateHandlerMethod[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    CodecType <: Codec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api: ref.c.WeakTypeTag
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    codec: ref.c.Expr[CodecType],
    backend: ref.c.Expr[Backend[Effect]],
    api: ref.c.Expr[Api]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[HandlerMethod[Node, Effect, Context]] = {
    import ref.c.universe.Quasiquote

    val invoke = generateInvoke[C, Node, CodecType, Effect, Context, Api](ref)(method, codec, backend, api)
    logBoundMethod[C, Api](ref)(method, invoke)
    ref.c.Expr[HandlerMethod[Node, Effect, Context]](q"""
      HandlerMethod(
        $invoke,
        ${method.lift.name},
        ${method.lift.resultType},
        ..${method.lift.parameters.flatMap(_.map(_.name))},
        ..${method.lift.parameters.flatMap(_.map(_.dataType))},
        ${methodUsesContext[C, Context](ref)(method)}
      )
    """)
  }

  private def generateInvoke[
    C <: blackbox.Context,
    Node: ref.c.WeakTypeTag,
    CodecType <: Codec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    Api
  ](ref: Reflection[C])(
    method: ref.RefMethod,
    codec: ref.c.Expr[CodecType],
    backend: ref.c.Expr[Backend[Effect]],
    api: ref.c.Expr[Api]
  )(implicit effectType: ref.c.WeakTypeTag[Effect[_]]): ref.c.Expr[(Seq[Node], Context) => Effect[Node]] = {
    import ref.c.universe.{weakTypeOf, Quasiquote}
    (weakTypeOf[Node], weakTypeOf[CodecType])

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create invoke function
    //   (argumentNodes: Seq[Node], context: Context) => Effect[Node]
    ref.c.Expr[(Seq[Node], Context) => Effect[Node]](q"""
      (argumentNodes: Seq[Node], context: Context) => ${
      // Create the method argument lists by decoding corresponding argument nodes into values
      //   List(List(
      //     codec.decode[Parameter0Type](argumentNodes(0)),
      //     ...
      //     codec.decode[ParameterNType](argumentNodes(N)) OR context
      //   )): List[List[ParameterXType]]
      val arguments = method.parameters.toList.zip(parameterListOffsets).map { case (parameters, offset) =>
        parameters.toList.zipWithIndex.map { case (parameter, index) =>
          if ((offset + index) == lastArgumentIndex && methodUsesContext[C, Context](ref)(method)) {
            q"context"
          } else {
            q"$codec.decode(argumentNodes[${parameter.dataType}](${offset + index}))"
          }
        }
      }

      // Create the API method call using the decoded arguments
      //   api.method(arguments ...): Effect[ResultValueType]
      val apiMethodCall = q"$api.${method.symbol}(..$arguments)"

      // Create encode result function
      //   (result: ResultValueType) => Node = codec.encode[ResultValueType](result)
      val resultValueType = unwrapType[C, Effect[_]](ref)(method.resultType)
      val encodeResult = q"(result: $resultValueType) => $codec.encode[$resultValueType](result)"

      // Create the effect mapping call using the method call and the encode result function
      //   backend.map(methodCall, encodeResult): Effect[Node]
      q"$backend.map($apiMethodCall, $encodeResult)"
    }
    """)
  }

  private def logBoundMethod[C <: blackbox.Context, Api: ref.c.WeakTypeTag](ref: Reflection[C])(
    method: ref.RefMethod,
    invoke: ref.c.Expr[Any]
  ): Unit = Option(System.getProperty(debugProperty)).foreach { _ =>
    println(
      s"""${methodSignature[C, Api](ref)(method)} =
        |  ${ref.c.universe.showCode(invoke.tree)}
        |""".stripMargin
    )
  }
}
