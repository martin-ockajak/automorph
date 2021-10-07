package automorph.handler

import automorph.log.MacroLogger
import automorph.spi.RpcProtocol.InvalidRequestException
import automorph.spi.{EffectSystem, MessageCodec}
import automorph.util.MethodReflection.functionToExpr
import automorph.util.{Method, MethodReflection, Reflection}
import scala.quoted.{Expr, Quotes, Type}
import scala.util.{Failure, Try}

/** RPC handler layer bindings code generation. */
private[automorph] object HandlerGenerator:

  /**
   * Generates handler bindings for all valid public methods of an API type.
   *
   * @param codec message codec plugin
   * @param system effect system plugin
   * @param api API instance
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @tparam Api API type
   * @return mapping of API method names to handler function bindings
   */
  inline def bindings[Node, Codec <: MessageCodec[Node], Effect[_], Context, Api <: AnyRef](
    codec: Codec,
    system: EffectSystem[Effect],
    api: Api
  ): Seq[HandlerBinding[Node, Effect, Context]] =
    ${ bindingsMacro[Node, Codec, Effect, Context, Api]('codec, 'system, 'api) }

  private def bindingsMacro[
    Node: Type,
    Codec <: MessageCodec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api <: AnyRef: Type
  ](
    codec: Expr[Codec],
    system: Expr[EffectSystem[Effect]],
    api: Expr[Api]
  )(using quotes: Quotes): Expr[Seq[HandlerBinding[Node, Effect, Context]]] =
    val ref = Reflection(quotes)

    // Detect and validate public methods in the API type
    val apiMethods = MethodReflection.apiMethods[Api, Effect](ref)
    val validMethods = apiMethods.flatMap(_.swap.toOption) match
      case Seq() => apiMethods.flatMap(_.toOption)
      case errors => ref.q.reflect.report.throwError(
          s"Failed to bind API methods:\n${errors.map(error => s"  $error").mkString("\n")}"
        )

    // Generate bound API method bindings
    val handlerBindings = validMethods.map { method =>
      generateBinding[Node, Codec, Effect, Context, Api](ref)(method, codec, system, api)
    }
    Expr.ofSeq(handlerBindings)

  private def generateBinding[
    Node: Type,
    Codec <: MessageCodec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api: Type
  ](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[Codec],
    system: Expr[EffectSystem[Effect]],
    api: Expr[Api]
  ): Expr[HandlerBinding[Node, Effect, Context]] =
    given Quotes = ref.q

    val invoke = generateInvoke[Node, Codec, Effect, Context, Api](ref)(method, codec, system, api)
    logBoundMethod[Api](ref)(method, invoke)
    '{
      HandlerBinding(
        ${ Expr(method.lift.rpcFunction) },
        $invoke,
        ${ Expr(MethodReflection.usesContext[Context](ref)(method)) }
      )
    }

  private def generateInvoke[
    Node: Type,
    Codec <: MessageCodec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api: Type
  ](ref: Reflection)(
    method: ref.RefMethod,
    codec: Expr[Codec],
    system: Expr[EffectSystem[Effect]],
    api: Expr[Api]
  ): Expr[(Seq[Node], Context) => Effect[Node]] =
    import ref.q.reflect.{asTerm, AppliedType, Term, TypeRepr}
    given Quotes = ref.q

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create invoke function
    //   (argumentNodes: Seq[Node], context: Context) => Effect[Node]
    '{ (argumentNodes, context) =>
      ${
        // Create the method argument lists by decoding corresponding argument nodes into values
        //   List(List(
        //     (Try(codec.decode[Parameter0Type](argumentNodes(0))) match {
        //       case Failure(error) => Failure(InvalidRequestException("Malformed argument: " + ${ Expr(parameter.name), error))
        //       case result => result
        //     }).get
        //     ...
        //     (Try(codec.decode[ParameterNType](argumentNodes(N))) match {
        //       case Failure(error) => Failure(InvalidRequestException("Malformed argument: " + ${ Expr(parameter.name), error))
        //       case result => result
        //     }).get
        //   )): List[List[ParameterXType]]
        val apiMethodArguments = method.parameters.toList.zip(parameterListOffsets).map((parameters, offset) =>
          parameters.toList.zipWithIndex.map { (parameter, index) =>
            val argumentIndex = offset + index
            if argumentIndex == lastArgumentIndex && MethodReflection.usesContext[Context](ref)(method) then
              // Use supplied context as a last argument if the method accepts context as its last parameter
              'context.asTerm
            else if argumentIndex > lastArgumentIndex then
              if MethodReflection.typeConstructor(ref.q)(parameter.dataType) =:= TypeRepr.of[Option] then
                // Use None if an optional argument is missing
                'None
              else
                // Raise error if a mandatory argument is missing
                '{ throw InvalidRequestException("Missing argument: " + ${ Expr(parameter.name) }) }
            else
              // Decode supplied argument node into a value
              val decodeArguments = List(List('{ argumentNodes(${ Expr(argumentIndex) }) }.asTerm))
              val decodeCall = MethodReflection.call(
                ref.q,
                codec.asTerm,
                "decode",
                List(parameter.dataType),
                decodeArguments
              )
              parameter.dataType.asType match
                case '[argumentType] => '{
                    (Try(${ decodeCall.asExprOf[argumentType] }) match
                      case Failure(error) =>
                        Failure(InvalidRequestException("Malformed argument: " + ${ Expr(parameter.name) }, error))
                      case result => result
                    ).get
                  }.asTerm
          }
        ).asInstanceOf[List[List[Term]]]

        // Create the API method call using the decoded arguments
        //   api.method(arguments*): Effect[ResultValueType]
        val apiMethodCall = MethodReflection.call(ref.q, api.asTerm, method.name, List.empty, apiMethodArguments)

        // Create encode result function
        //   (result: ResultValueType) => Node = codec.encode[ResultValueType](result)
        val resultValueType = MethodReflection.unwrapType[Effect](ref.q)(method.resultType).dealias
        val encodeResult = resultValueType.asType match
          case '[resultType] => '{ (result: resultType) =>
              ${
                MethodReflection.call(
                  ref.q,
                  codec.asTerm,
                  "encode",
                  List(resultValueType),
                  List(List('{ result }.asTerm))
                ).asExprOf[Node]
              }
            }

        // Create the effect mapping call using the method call and the encode result function
        //   system.map(apiMethodCall, encodeResult): Effect[Node]
        val mapArguments = List(List(apiMethodCall, encodeResult.asTerm))
        MethodReflection.call(
          ref.q,
          system.asTerm,
          "map",
          List(resultValueType, TypeRepr.of[Node]),
          mapArguments
        ).asExprOf[Effect[Node]]
      }
    }

  private def logBoundMethod[Api: Type](ref: Reflection)(method: ref.RefMethod, invoke: Expr[Any]): Unit =
    import ref.q.reflect.{asTerm, Printer}

    MacroLogger.debug(
      s"""${MethodReflection.signature[Api](ref)(method)} =
        |  ${invoke.asTerm.show(using Printer.TreeShortCode)}
        |""".stripMargin
    )
