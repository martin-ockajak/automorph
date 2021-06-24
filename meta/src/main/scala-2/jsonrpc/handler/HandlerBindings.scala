package jsonrpc.handler

import jsonrpc.protocol.MethodBindings.{methodSignature, methodUsesContext, validApiMethods}
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.util.Reflection
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** JSON-RPC handler layer bindings code generation. */
private[jsonrpc] case object HandlerBindings {

  private val debugProperty = "jsonrpc.macro.debug"
//  private val debugDefault = "true"
  private val debugDefault = ""

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
   * @tparam ApiType API type
   * @return mapping of method names to handler method bindings
   */
  def generate[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType,
    backend: Backend[Effect],
    api: ApiType
  ): Map[String, HandlerMethod[Node, Effect, Context]] = macro generateExpr[Node, CodecType, Effect, Context, ApiType]

  def generateExpr[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag,
    ApiType <: AnyRef: c.WeakTypeTag
  ](c: blackbox.Context)(
    codec: c.Expr[CodecType],
    backend: c.Expr[Backend[Effect]],
    api: c.Expr[ApiType]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[Map[String, HandlerMethod[Node, Effect, Context]]] = {
    import c.universe._
    val ref = Reflection(c)

    // Detect and validate public methods in the API type
    val apiMethods = validApiMethods[ApiType, Effect[_]](ref)
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if (invalidMethodErrors.nonEmpty) {
      ref.c.abort(
        ref.c.enclosingPosition,
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )
    }

    // Generate bound API method bindings
    val handlerMethods = validMethods.map { method =>
      generateHandlerMethod[Node, CodecType, Effect, Context, ApiType](c, ref)(method, codec, backend, api)
    }
    c.Expr[Map[String, HandlerMethod[Node, Effect, Context]]](q"""
      Seq(..$handlerMethods).toMap
    """)
  }

  private def generateHandlerMethod[
    Node: ref.c.WeakTypeTag,
    CodecType <: Codec[Node]: ref.c.WeakTypeTag,
    Effect[_],
    Context: ref.c.WeakTypeTag,
    ApiType: ref.c.WeakTypeTag
  ](c: blackbox.Context, ref: Reflection)(
    method: ref.RefMethod,
    codec: c.Expr[CodecType],
    backend: c.Expr[Backend[Effect]],
    api: c.Expr[ApiType]
  )(implicit effectType: c.WeakTypeTag[Effect[_]]): c.Expr[(String, HandlerMethod[Node, Effect, Context])] = {
    import c.universe._

    val liftedMethod = method.lift
//    val invoke = generateInvokeFunction[Node, CodecType, Effect, Context, ApiType](ref, method, codec, backend, api)
    val name = q"${liftedMethod.name}"
    val resultType = q"${liftedMethod.resultType}"
    val parameterNames = q"..${liftedMethod.parameters.flatMap(_.map(_.name))}"
    val parameterTypes = q"..${liftedMethod.parameters.flatMap(_.map(_.dataType))}"
    val usesContext = q"${methodUsesContext[Context](ref)(method)}"
//    logBoundMethod[ApiType](ref)(method, invoke)
//    '
//    {
//    $name -> HandlerMethod($invoke, $name, $resultType, $parameterNames, $parameterTypes, $usesContext)
//    }
    null
  }

//  private def generateInvokeFunction[
//    Node: Type,
//    CodecType <: Codec[Node]: Type,
//    Effect[_]: Type,
//    Context: Type,
//    ApiType: Type
//  ](
//    ref: Reflection,
//    method: ref.RefMethod,
//    codec: Expr[CodecType],
//    backend: Expr[Backend[Effect]],
//    api: Expr[ApiType]
//  ): Expr[(Seq[Node], Context) => Effect[Node]] = {
//    import ref.q.reflect.{asTerm, Term, TypeRepr}
//    given Quotes = ref.q
//
//    // Map multiple parameter lists to flat argument node list offsets
//    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
//      indices :+ (indices.last + size)
//    }
//    val lastArgumentIndex = method.parameters.map(_.size).sum - 1
//
//    // Create invoke function
//    //   (argumentNodes: Seq[Node], context: Context) => Effect[Node]
//    '{ (argumentNodes: Seq[Node], context: Context) =>
//      ${
//        // Create the method argument lists by decoding corresponding argument nodes into values
//        //   List(List(
//        //     codec.decode[Parameter0Type](argumentNodes(0)),
//        //     codec.decode[Parameter1Type](argumentNodes(1)),
//        //     ...
//        //     codec.decode[ParameterNType](argumentNodes(N)) OR context
//        //   )): List[List[ParameterXType]]
//        val argumentLists = method.parameters.toList.zip(parameterListOffsets).map((parameters, offset) =>
//          parameters.toList.zipWithIndex.map { (parameter, index) =>
//            val argumentNode = '{ argumentNodes(${ Expr(offset + index) }) }
//            if (offset + index) == lastArgumentIndex && methodUsesContext[Context](ref, method) then
//              'context.asTerm
//            else
//              call(ref.q, codec.asTerm, "decode", List(parameter.dataType), List(List(argumentNode.asTerm)))
//          }
//        ).asInstanceOf[List[List[Term]]]
//
//        // Create the API method call using the decoded arguments
//        //   api.method(decodedArguments ...): Effect[ResultValueType]
//        val apiMethodCall = call(ref.q, api.asTerm, method.name, List.empty, argumentLists)
//
//        // Create encode result function
//        //   (result: ResultValueType) => Node = codec.encode[ResultValueType](result)
//        val resultValueType = unwrapType[Effect](ref, method.resultType)
////        val encodeResult = (TypeRepr.of[CodecType].dealias.asType, resultValueType.asType) match
////          case ('[codecType], '[resultType]) => '{ (result: resultType) =>
////              $codec.asInstanceOf[codecType].encode(result)
////            }
//
//        val encodeResult = resultValueType.asType match
//          case '[resultType] => '{ (result: resultType) =>
//              ${
//                call(ref.q, codec.asTerm, "encode", List(resultValueType), List(List('{ result }.asTerm))).asExprOf[Node]
//              }
//            }
//
//        // Create the effect mapping call using the method call and the encode result function
//        //   backend.map(methodCall, encodeResult): Effect[Node]
//        val mapArguments = List(List(apiMethodCall, encodeResult.asTerm))
//        call(ref.q, backend.asTerm, "map", List(resultValueType, TypeRepr.of[Node]), mapArguments).asExprOf[Effect[Node]]
//      }
//    }
//  }
//
//
  private def logBoundMethod[ApiType: ref.c.WeakTypeTag](ref: Reflection)(
    method: ref.RefMethod,
    invoke: ref.c.Expr[Any]
  ): Unit = {
    import ref.c.universe.showCode

    if (Option(System.getenv(debugProperty)).getOrElse(debugDefault).nonEmpty) {
      println(
        s"""${methodSignature[ApiType](ref)(method)} =
          |  ${showCode(invoke.tree)}
          |""".stripMargin
      )
    }
  }
}
