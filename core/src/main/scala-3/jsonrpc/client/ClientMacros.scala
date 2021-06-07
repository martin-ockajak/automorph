package jsonrpc.client

import jsonrpc.client.ClientMethod
import jsonrpc.core.ApiReflection.{callMethodTerm, detectApiMethods, methodDescription, methodUsesContext, effectResultType}
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.util.Reflection
import scala.quoted.{Expr, Quotes, Type, quotes}

case object ClientMacros:
  private val debugProperty = "jsonrpc.macro.debug"
  private val debugDefault = "true"
//  private val debugDefault = ""

  /**
   * Generate client bindings for all valid public methods of an API type.
   *
   * @param codec message format codec plugin
   * @tparam Node message format node representation type
   * @tparam CodecType message format codec type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam ApiType API type
   * @return mapping of method names to their JSON-RPC wrapper functions
   */
  inline def bind[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType
  ): ApiType =
    ${ bind[Node, CodecType, Effect, Context, ApiType]('codec) }

  private def bind[Node: Type, CodecType <: Codec[Node]: Type, Effect[_]: Type, Context: Type, ApiType <: AnyRef: Type](
    codec: Expr[CodecType]
  )(using quotes: Quotes): Expr[ApiType] =
    import ref.quotes.reflect.{Block, Printer, Symbol, TypeDef, TypeRepr, TypeTree, asTerm}
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = detectApiMethods[Effect](ref, TypeTree.of[ApiType])
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if invalidMethodErrors.nonEmpty then
      ref.quotes.reflect.report.throwError(
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )

    // Debug prints
//    println(proxy.asTerm.show(using Printer.TreeAnsiCode))
//    println(proxy.asTerm)

    '{
      null
    }.asInstanceOf[Expr[ApiType]]

    // Generate bound API method bindings
//    val clientMethods = Expr.ofSeq(validMethods.map { method =>
//      generateClientMethod[Node, CodecType, Effect, Context](ref, method, codec, backend, api)
//    })
//    '{ $clientMethods.toMap[String, ClientMethod[Node, Effect, Context]] }

  private def generateClientMethod[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType]
  ): Expr[(String, ClientMethod[Node, Context])] =
    given Quotes = ref.quotes

    val liftedMethod = method.lift
    val encodeArguments = generateEncodeArgumentsFunction[Node, CodecType, Context](ref, method, codec)
    val decodeResult = generateDecodeResultFunction[Node, CodecType, Effect](ref, method, codec)
    val name = Expr(liftedMethod.name)
    val resultType = Expr(liftedMethod.resultType)
    val parameterNames = Expr(liftedMethod.parameters.flatMap(_.map(_.name)))
    val parameterTypes = Expr(liftedMethod.parameters.flatMap(_.map(_.dataType)))
    val usesContext = Expr(methodUsesContext[Context](ref, method))
    '{
      $name -> ClientMethod($encodeArguments, $decodeResult, $name, $resultType, $parameterNames, $parameterTypes, $usesContext)
    }

  private def generateEncodeArgumentsFunction[Node: Type, CodecType <: Codec[Node]: Type, Context: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType]
  ): Expr[(Seq[Any], Context) => Seq[Node]] = ???

  private def generateDecodeResultFunction[Node: Type, CodecType <: Codec[Node]: Type, Effect[_]: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType]
  ): Expr[Node => Any] =
    import ref.quotes.reflect.{AppliedType, IntConstant, Lambda, Literal, MethodType, Symbol, Term, TypeRepr, asTerm}
    given Quotes = ref.quotes

    // Create decode result function
    //   (resultNode: Node) => ResultValueType = codec.dencode[ResultValueType](resultNode)
    val resultValueType = effectResultType[Effect](ref, method)
    val decodeResultType = MethodType(List("resultNode"))(_ => List(TypeRepr.of[Node]), _ => resultValueType)
    Lambda(
      Symbol.spliceOwner,
      decodeResultType,
      (symbol, arguments) =>
        callMethodTerm(ref.quotes, codec.asTerm, "decode", List(resultValueType), List(arguments))
    ).asExprOf[Node => Any]
