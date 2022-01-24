package automorph.handler.meta

import automorph.Contextual
import automorph.handler.HandlerBinding
import automorph.log.MacroLogger
import automorph.reflection.MethodReflection.functionToExpr
import automorph.reflection.{ClassReflection, MethodReflection}
import automorph.spi.RpcProtocol.InvalidRequestException
import automorph.spi.{EffectSystem, MessageCodec}
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
   * @tparam Context message context type
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
    val ref = ClassReflection(quotes)

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
  ](ref: ClassReflection)(
    method: ref.RefMethod,
    codec: Expr[Codec],
    system: Expr[EffectSystem[Effect]],
    api: Expr[Api]
  ): Expr[HandlerBinding[Node, Effect, Context]] =
    given Quotes = ref.q

    val argumentDecoders = generateArgumentDecoders[Node, Codec, Context](ref)(method, codec)
    val encodeResult = generateEncodeResult[Node, Codec, Effect, Context](ref)(method, codec)
    val call = generateCall[Effect, Context, Api](ref)(method, api)
    val invoke = generateInvoke[Node, Codec, Effect, Context, Api](ref)(method, codec, system, api)
    logBoundMethod[Api](ref)(method, invoke)
    '{
      HandlerBinding(
        ${ Expr(method.lift.rpcFunction) },
        $argumentDecoders,
        $encodeResult,
        $call,
        $invoke,
        ${ Expr(MethodReflection.acceptsContext[Context](ref)(method)) }
      )
    }

  private def generateArgumentDecoders[Node: Type, Codec <: MessageCodec[Node]: Type, Context: Type](ref: ClassReflection)(
    method: ref.RefMethod,
    codec: Expr[Codec]
  ): Expr[Map[String, Option[Node] => Any]] =
    import ref.q.reflect.{Term, TypeRepr, asTerm}
    given Quotes = ref.q

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create encoded non-existent value expression
    //  codec.encode(None)
    val encodeNoneCall = MethodReflection.call(
      ref.q,
      codec.asTerm,
      MessageCodec.encodeMethod,
      List(TypeRepr.of[None.type]),
      List(List('{ None }.asTerm))
    )

    // Create a map of method parameter names to functions decoding method argument node into a value
    //   Map(
    //     parameterNName -> ((argumentNode: Node) =>
    //       codec.decode[ParameterNType](argumentNode.getOrElse(codec.encode(None)))
    //     ...
    //   ): Map[String, Node => Any]
    val argumentDecoders = method.parameters.toList.zip(parameterListOffsets).flatMap((parameters, offset) =>
      parameters.toList.zipWithIndex.flatMap { (parameter, index) =>
        Option.when((offset + index) != lastArgumentIndex || !MethodReflection.acceptsContext[Context](ref)(method)) {
          '{
            ${ Expr(parameter.name) } -> (
              (argumentNode: Option[Node]) => ${
                // Decode an argument node if present or empty node if missing into a value
                val decodeArguments = List(List('{
                  argumentNode.getOrElse(${ encodeNoneCall.asExprOf[Node] })
                }.asTerm))
                MethodReflection.call(
                  ref.q,
                  codec.asTerm,
                  MessageCodec.decodeMethod,
                  List(parameter.dataType),
                  decodeArguments
                ).asExprOf[Any]
              }
            )
          }
        }
      }
    )
    '{ Map(${ Expr.ofSeq(argumentDecoders) }*) }

  private def generateEncodeResult[Node: Type, Codec <: MessageCodec[Node]: Type, Effect[_]: Type, Context: Type](
    ref: ClassReflection
  )(method: ref.RefMethod, codec: Expr[Codec]): Expr[Any => (Node, Option[Context])] =
    import ref.q.reflect.asTerm
    given Quotes = ref.q

    // Create a result encoding function
    //   (result: Any) =>
    //     codec.encode[ResultType](result.asInstanceOf[ResultType]) -> Option.empty[Context]
    //       OR
    //   (result: Any) =>
    //     codec.encode[ContextualResultType](result.asInstanceOf[ResultType].result) -> Some(
    //       result.asInstanceOf[ResultType].context
    //     )
    val resultType = MethodReflection.unwrapType[Effect](ref.q)(method.resultType).dealias
    MethodReflection.contextualResult[Context, Contextual](ref.q)(resultType).map { contextualResultType =>
      contextualResultType.asType match
        case '[resultValueType] => '{
          (result: Any) => ${
            MethodReflection.call(
              ref.q,
              codec.asTerm,
              MessageCodec.encodeMethod,
              List(contextualResultType),
              List(List('{ result.asInstanceOf[Contextual[resultValueType, Context]].result }.asTerm))
            ).asExprOf[Node]
          } -> Some(result.asInstanceOf[Contextual[resultValueType, Context]].context)
        }
    }.getOrElse {
      resultType.asType match
        case '[resultValueType] => '{
          (result: Any) => ${
            MethodReflection.call(
              ref.q,
              codec.asTerm,
              MessageCodec.encodeMethod,
              List(resultType),
              List(List('{ result.asInstanceOf[resultValueType] }.asTerm))
            ).asExprOf[Node]
          } -> Option.empty[Context]
        }
    }

  private def generateCall[Effect[_]: Type, Context: Type, Api: Type](ref: ClassReflection)(
    method: ref.RefMethod,
    api: Expr[Api]
  ): Expr[(Seq[Any], Context) => Effect[Any]] =
    import ref.q.reflect.{Term, TypeRepr, asTerm}
    given Quotes = ref.q

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create API method call function
    //   (arguments: Seq[Any], requestContext: Context) => Effect[Any]
    val resultType = MethodReflection.unwrapType[Effect](ref.q)(method.resultType).dealias
    '{ (arguments, requestContext) =>
      ${
        // Create the method argument lists by type coercing supplied arguments
        // List(List(
        //   arguments(N).asInstanceOf[Any]
        // )): List[List[ParameterXType]]
        val apiMethodArguments = method.parameters.toList.zip(parameterListOffsets).map((parameters, offset) =>
          parameters.toList.zipWithIndex.map { (parameter, index) =>
            val argumentIndex = offset + index
            if argumentIndex == lastArgumentIndex && MethodReflection.acceptsContext[Context](ref)(method) then
              // Use supplied request context as a last argument if the method accepts context as its last parameter
              'requestContext.asTerm
            else
              // Coerce argument type
              parameter.dataType.asType match
                case '[parameterType] => '{
                  arguments(${ Expr(argumentIndex) }).asInstanceOf[parameterType]
                }.asTerm
          }
        ).asInstanceOf[List[List[Term]]]

        // Call the API method and type coerce the result
        //   api.method(arguments*).asInstanceOf[Effect[Any]]: Effect[Any]
        resultType.asType match
          case '[resultValueType] => '{
            ${
              MethodReflection.call(
                ref.q,
                api.asTerm,
                method.name,
                List.empty,
                apiMethodArguments
              ).asExprOf[Effect[resultValueType]]
            }.asInstanceOf[Effect[Any]]
          }
      }
    }

  private def generateInvoke[
    Node: Type,
    Codec <: MessageCodec[Node]: Type,
    Effect[_]: Type,
    Context: Type,
    Api: Type
  ](ref: ClassReflection)(
    method: ref.RefMethod,
    codec: Expr[Codec],
    system: Expr[EffectSystem[Effect]],
    api: Expr[Api]
  ): Expr[(Seq[Option[Node]], Context) => Effect[(Node, Option[Context])]] =
    import ref.q.reflect.{Term, TypeRepr, asTerm}
    given Quotes = ref.q

    // Map multiple parameter lists to flat argument node list offsets
    val parameterListOffsets = method.parameters.map(_.size).foldLeft(Seq(0)) { (indices, size) =>
      indices :+ (indices.last + size)
    }
    val lastArgumentIndex = method.parameters.map(_.size).sum - 1

    // Create encoded non-existent value expression
    val encodeNoneArguments = List(List('{ None }.asTerm))
    val encodeNoneCall = MethodReflection.call(
      ref.q,
      codec.asTerm,
      MessageCodec.encodeMethod,
      List(TypeRepr.of[None.type]),
      encodeNoneArguments
    )

    // Create invoke function
    //   (argumentNodes: Seq[Option[Node]], requestContext: Context) => Effect[Node]
    '{ (argumentNodes, requestContext) =>
      ${
        // Create the method argument lists by decoding corresponding argument nodes into values
        //   List(List(
        //     (Try(Option(codec.decode[Parameter0Type](argumentNodes(0).getOrElse(codec.encode(None)))).get) match {
        //       ... error handling ...
        //     }).get
        //     ...
        //     (Try(Option(codec.decode[ParameterNType](argumentNodes(N).getOrElse(codec.encode(None)))).get) match {
        //       ... error handling ...
        //     }).get
        //   )): List[List[ParameterXType]]
        val apiMethodArguments = method.parameters.toList.zip(parameterListOffsets).map((parameters, offset) =>
          parameters.toList.zipWithIndex.map { (parameter, index) =>
            val argumentIndex = offset + index
            if argumentIndex == lastArgumentIndex && MethodReflection.acceptsContext[Context](ref)(method) then
              // Use supplied request context as a last argument if the method accepts context as its last parameter
              'requestContext.asTerm
            else
              // Decode an argument node if present or otherwise an empty node into a value
              val decodeArguments = List(List('{
                argumentNodes(${ Expr(argumentIndex) }).getOrElse(${ encodeNoneCall.asExprOf[Node] })
              }.asTerm))
              val decodeCall = MethodReflection.call(
                ref.q,
                codec.asTerm,
                MessageCodec.decodeMethod,
                List(parameter.dataType),
                decodeArguments
              )
              parameter.dataType.asType match
                case '[argumentType] => '{
                    (Try(Option(${ decodeCall.asExprOf[argumentType] }).get) match
                      case Failure(error) =>
                        Failure(InvalidRequestException(
                          argumentNodes(${ Expr(argumentIndex) }).fold("Missing")(_ => "Malformed") + " argument: " + ${
                            Expr(parameter.name)
                          },
                          error
                        ))
                      case result => result
                    ).get
                  }.asTerm
          }
        ).asInstanceOf[List[List[Term]]]

        // Create the API method call using the decoded arguments
        //   api.method(arguments*): Effect[ResultValueType]
        val apiMethodCall = MethodReflection.call(ref.q, api.asTerm, method.name, List.empty, apiMethodArguments)

        // Create encode result function
        //   (result: ResultValueType) => Node = codec.encode[ResultType](result) -> Option.empty[Context]
        val resultType = MethodReflection.unwrapType[Effect](ref.q)(method.resultType).dealias
        val encodeResult =
          MethodReflection.contextualResult[Context, Contextual](ref.q)(resultType).map { contextualResultType =>
            contextualResultType.asType match
              case '[resultValueType] => '{ (result: Contextual[resultValueType, Context]) =>
                  ${
                    MethodReflection.call(
                      ref.q,
                      codec.asTerm,
                      MessageCodec.encodeMethod,
                      List(contextualResultType),
                      List(List('{ result.result }.asTerm))
                    ).asExprOf[Node]
                  } -> Some(result.context)
                }
          }.getOrElse {
            resultType.asType match
              case '[resultValueType] => '{ (result: resultValueType) =>
                  ${
                    MethodReflection.call(
                      ref.q,
                      codec.asTerm,
                      MessageCodec.encodeMethod,
                      List(resultType),
                      List(List('{ result }.asTerm))
                    ).asExprOf[Node]
                  } -> Option.empty[Context]
                }
          }

        // Create the effect mapping call using the method call and the encode result function
        //   system.map(apiMethodCall, encodeResult): Effect[(Node, Option[Context])]
        val mapArguments = List(List(apiMethodCall), List(encodeResult.asTerm))
        MethodReflection.call(
          ref.q,
          system.asTerm,
          "map",
          List(resultType, TypeRepr.of[(Node, Option[Context])]),
          mapArguments
        ).asExprOf[Effect[(Node, Option[Context])]]
      }
    }

  private def logBoundMethod[Api: Type](ref: ClassReflection)(method: ref.RefMethod, invoke: Expr[Any]): Unit =
    import ref.q.reflect.{Printer, asTerm}

    MacroLogger.debug(
      s"""${MethodReflection.signature[Api](ref)(method)} =
        |  ${invoke.asTerm.show(using Printer.TreeShortCode)}
        |""".stripMargin
    )
