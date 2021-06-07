package jsonrpc.handler

import jsonrpc.core.ApiReflection.{callMethodTerm, detectApiMethods, methodDescription, methodUsesContext}
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.util.Reflection
import scala.quoted.{Expr, Quotes, Type, quotes}

case object HandlerMacros:

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
   * @return mapping of method names to their JSON-RPC wrapper functions
   */
  inline def bind[Node, CodecType <: Codec[Node], Effect[_], Context, ApiType <: AnyRef](
    codec: CodecType,
    backend: Backend[Effect],
    api: ApiType
  ): Map[String, HandlerMethod[Node, Effect, Context]] =
    ${ bind[Node, CodecType, Effect, Context, ApiType]('codec, 'backend, 'api) }

  private def bind[Node: Type, CodecType <: Codec[Node]: Type, Effect[_]: Type, Context: Type, ApiType <: AnyRef: Type](
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  )(using quotes: Quotes): Expr[Map[String, HandlerMethod[Node, Effect, Context]]] =
    import ref.quotes.reflect.{TypeRepr, TypeTree, asTerm}
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = detectApiMethods[Effect](ref, TypeTree.of[ApiType])
    val validMethods = apiMethods.flatMap(_.toOption)
    val invalidMethodErrors = apiMethods.flatMap(_.swap.toOption)
    if invalidMethodErrors.nonEmpty then
      ref.quotes.reflect.report.throwError(
        s"Failed to bind API methods:\n${invalidMethodErrors.map(error => s"  $error").mkString("\n")}"
      )

    // Generate bound API method bindings
    val handlerMethods = Expr.ofSeq(validMethods.map { method =>
      generateHandlerMethod[Node, CodecType, Effect, Context, ApiType](ref, method, codec, backend, api)
    })
    '{ $handlerMethods.toMap[String, HandlerMethod[Node, Effect, Context]] }

  private def generateHandlerMethod[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  ): Expr[(String, HandlerMethod[Node, Effect, Context])] =
    given Quotes = ref.quotes

    val liftedMethod = method.lift
    val invoke = generateInvokeFunction[Node, CodecType, Effect, Context, ApiType](ref, method, codec, backend, api)
    val name = Expr(liftedMethod.name)
    val resultType = Expr(liftedMethod.resultType)
    val parameterNames = Expr(liftedMethod.parameters.flatMap(_.map(_.name)))
    val parameterTypes = Expr(liftedMethod.parameters.flatMap(_.map(_.dataType)))
    val usesContext = Expr(methodUsesContext[Context](ref, method))
    logBoundMethod[ApiType](ref, method, invoke)
    '{
      $name -> HandlerMethod($invoke, $name, $resultType, $parameterNames, $parameterTypes, $usesContext)
    }

  private def generateInvokeFunction[
    Node: Type,
    CodecType <: Codec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    ApiType: Type
  ](
    ref: Reflection,
    method: ref.QuotedMethod,
    codec: Expr[CodecType],
    backend: Expr[Backend[Effect]],
    api: Expr[ApiType]
  ): Expr[(Seq[Node], Context) => Effect[Node]] =
    import ref.quotes.reflect.{AppliedType, IntConstant, Lambda, Literal, MethodType, Symbol, Term, TypeRepr, asTerm}
    given Quotes = ref.quotes

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Determine the method result value type
    val effectType = TypeRepr.of[Effect]
    val resultValueType =
      method.resultType match
        case appliedType: AppliedType if appliedType.tycon =:= effectType => appliedType.args.last
        case otherType                                                    => otherType

    // Create binding function
    //   (argumentNodes: Seq[Node], context: Context) => Effect[Node]
    val bindingType = MethodType(List("argumentNodes", "context"))(
      _ => List(TypeRepr.of[Seq[Node]], TypeRepr.of[Context]),
      _ => effectType.appliedTo(TypeRepr.of[Node])
    )
    Lambda(
      Symbol.spliceOwner,
      bindingType,
      (symbol, arguments) =>
        // Create the method argument lists by decoding corresponding argument nodes into required parameter types
        //   List(List(
        //     codec.decode[Parameter0Type](argumentNodes(0)),
        //     codec.decode[Parameter1Type](argumentNodes(1)),
        //     ...
        //     codec.decode[ParameterNType](argumentNodes(N)) OR context
        //   )): List[List[ParameterXType]]
        val List(argumentNodes, context) = arguments
        val argumentLists = method.parameters.toList.zip(parameterListOffsets).map((parameters, offset) =>
          parameters.toList.zipWithIndex.map { (parameter, index) =>
            val argumentIndex = Literal(IntConstant(offset + index))
            val argumentNode =
              callMethodTerm(ref.quotes, argumentNodes.asInstanceOf[Term], "apply", List.empty, List(List(argumentIndex)))
            if (offset + index) == lastArgumentIndex && methodUsesContext[Context](ref, method) then
              context
            else
              callMethodTerm(ref.quotes, codec.asTerm, "decode", List(parameter.dataType), List(List(argumentNode)))
          }
        ).asInstanceOf[List[List[Term]]]

        // Create the method call using the decoded arguments
        //   api.method(decodedArguments ...): Effect[ResultValueType]
        val methodCall = callMethodTerm(ref.quotes, api.asTerm, method.name, List.empty, argumentLists)

        // Create encode result function
        //   (result: ResultValue) => Node = codec.encode[ResultValueType](result)
        val encodeResultType = MethodType(List("result"))(_ => List(resultValueType), _ => TypeRepr.of[Node])
        val encodeResult = Lambda(
          symbol,
          encodeResultType,
          (symbol, arguments) =>
            callMethodTerm(ref.quotes, codec.asTerm, "encode", List(resultValueType), List(arguments))
        )

        // Create an effect mapping call using the method call and the encode result function
        //   backend.map(methodCall, encodeResult): Effect[Node]
        callMethodTerm(
          ref.quotes,
          backend.asTerm,
          "map",
          List(resultValueType, TypeRepr.of[Node]),
          List(List(methodCall, encodeResult))
        )
    ).asExprOf[(Seq[Node], Context) => Effect[Node]]

  private def logBoundMethod[ApiType: Type](
    ref: Reflection,
    method: ref.QuotedMethod,
    bindingFunction: Expr[Any]
  ): Unit =
    import ref.quotes.reflect.{asTerm, Printer, TypeRepr}

    if Option(System.getenv(debugProperty)).getOrElse(debugDefault).nonEmpty then
      println(
        s"${methodDescription[ApiType](ref, method)} = \n  ${bindingFunction.asTerm.show(using Printer.TreeAnsiCode)}\n"
      )
