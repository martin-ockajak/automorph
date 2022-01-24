package automorph

import automorph.handler.meta.HandlerMeta
import automorph.handler.{HandlerBinding, HandlerResult, ProtocolHandlerBuilder, SystemHandlerBuilder}
import automorph.log.{LogProperties, Logging}
import automorph.spi.RpcProtocol.FunctionNotFoundException
import automorph.spi.protocol.{RpcFunction, RpcMessage, RpcRequest}
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.Extensions.{EffectOps, TryOps}
import automorph.util.Bytes
import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

/**
 * RPC request handler.
 *
 * Used by RPC servers to invoke bound API methods based on incoming requests.
 *
 * @constructor Creates a new RPC request handler with specified system and protocol plugins providing corresponding message context type.
 * @param protocol RPC protocol plugin
 * @param system effect system plugin
 * @param mapName maps API schema function to the exposed RPC function name (empty result causes the method not to be exposed)
 * @param apiBindings API method bindings
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context message context type
 */
final case class Handler[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  protocol: RpcProtocol[Node, Codec, Context],
  system: EffectSystem[Effect],
  mapName: String => Iterable[String] = Seq(_),
  apiBindings: ListMap[String, HandlerBinding[Node, Effect, Context]] =
    ListMap[String, HandlerBinding[Node, Effect, Context]]()
) extends HandlerMeta[Node, Codec, Effect, Context] with Logging {

  private val bindings = (schemaBindings ++ apiBindings).flatMap { case (name, binding) =>
    mapName(name).map(_ -> binding)
  }
  implicit private val givenSystem: EffectSystem[Effect] = system

  /** Bound RPC functions. */
  lazy val functions: Seq[RpcFunction] = bindings.map { case (name, binding) =>
    binding.function.copy(name = name)
  }.toSeq

  /**
   * Processes an RPC request by invoking a bound RPC function based on the specified RPC request and its context and return an RPC response.
   *
   * @param requestBody request message body
   * @param requestContext request context
   * @param requestId request correlation identifier
   * @tparam MessageBody message body type
   * @return optional response message
   */
  def processRequest[MessageBody: Bytes](
    requestBody: MessageBody,
    requestContext: Context,
    requestId: String
  ): Effect[HandlerResult[MessageBody, Context]] = {
    // Parse request
    val requestMessageBody = implicitly[Bytes[MessageBody]].from(requestBody)
    protocol.parseRequest(requestMessageBody, requestContext, requestId).fold(
      error => errorResponse(error.exception, error.message, responseRequired = true, ListMap(LogProperties.requestId -> requestId)),
      rpcRequest => {
        // Invoke requested RPC function
        lazy val requestProperties = ListMap(
          LogProperties.requestId -> requestId
        ) ++ rpcRequest.message.properties + (
          LogProperties.messageSize -> requestMessageBody.length.toString
        )
        lazy val allProperties = requestProperties ++ rpcRequest.message.text.map(LogProperties.messageBody -> _)
        logger.trace(s"Received ${protocol.name} request", allProperties)
        callFunction(rpcRequest, requestContext, requestProperties)
      }
    )
  }

  /**
   * Creates a copy of this handler with specified global bound API method name mapping function.
   *
   * Bound API methods are exposed using their transformed via the `mapName` function.
   * The `mapName` function is applied globally to the results to all bound APIs and their specific name mapping provided by the 'bind' method.
   *
   * @param mapName maps API method name to the exposed RPC function name (empty result causes the method not to be exposed)
   * @return RPC request handler with specified global API method name mapping
   */
  def mapName(mapName: String => Iterable[String]): Handler[Node, Codec, Effect, Context] =
    copy(mapName = mapName)

  override def toString: String = {
    val plugins = Map(
      "system" -> system,
      "protocol" -> protocol
    ).map { case (name, plugin) => s"$name = ${plugin.getClass.getName}" }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }

  /**
   * Calls bound RPC function specified in a request and creates a response.
   *
   * Optional request context is used as a last RPC function argument.
   *
   * @param rpcRequest RPC request
   * @param context request context
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return bound function call RPC response
   */
  private def callFunction[MessageBody: Bytes](
    rpcRequest: RpcRequest[Node, protocol.Metadata],
    context: Context,
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] = {
    // Lookup bindings for the specified RPC function
    val responseRequred = rpcRequest.responseRequired
    logger.debug(s"Processing ${protocol.name} request", requestProperties)
    bindings.get(rpcRequest.function).map { binding =>
      // Extract arguments
      extractArguments(rpcRequest, binding).pureFold(
        error => errorResponse(error, rpcRequest.message, responseRequred, requestProperties),
        arguments =>
          // Invoke bound function
          Try(binding.invoke(arguments, context).either).pureFold(
            error => errorResponse(error, rpcRequest.message, responseRequred, requestProperties),
            result => resultResponse(result, rpcRequest, requestProperties)
          )
      )
    }.getOrElse {
      val error = FunctionNotFoundException(s"Function not found: ${rpcRequest.function}", None.orNull)
      errorResponse(error, rpcRequest.message, responseRequred, requestProperties)
    }
  }

  /**
   * Validates and extracts specified bound RPC function arguments from a request.
   *
   * Optional request context is used as a last RPC function argument.
   *
   * @param rpcRequest RPC request
   * @param binding handler RPC function binding
   * @return bound function arguments
   */
  private def extractArguments(
    rpcRequest: RpcRequest[Node, ?],
    binding: HandlerBinding[Node, Effect, Context]
  ): Try[Seq[Option[Node]]] = {
    // Adjust expected function parameters if it uses context as its last parameter
    val parameters = binding.function.parameters
    val parameterNames = parameters.map(_.name).dropRight(if (binding.acceptsContext) 1 else 0)

    // Identify redundant arguments
    val namedArguments = rpcRequest.arguments.flatMap(_.toOption).toMap
    val positionalArguments = rpcRequest.arguments.flatMap(_.swap.toOption)
    val argumentNames = namedArguments.keys.toSeq
    val matchedNamedArguments = argumentNames.intersect(parameterNames)
    val requiredPositionalArguments = parameterNames.size - matchedNamedArguments.size
    val redundantNames = argumentNames.diff(parameterNames)
    val redundantIndices = Range(requiredPositionalArguments, positionalArguments.size)

    // Assemble required arguments
    if (redundantNames.size + redundantIndices.size > 0) {
      val redundantIdentifiers = redundantNames ++ redundantIndices.map(_.toString)
      Failure(new IllegalArgumentException(s"Redundant arguments: ${redundantIdentifiers.mkString(", ")}"))
    } else {
      Success(parameterNames.foldLeft(Seq[Option[Node]]() -> 0) { case ((arguments, index), name) =>
        val (argument, newIndex) = namedArguments.get(name) match {
          case Some(value) => Some(value) -> index
          case _ if index < positionalArguments.size => Some(positionalArguments(index)) -> (index + 1)
          case _ => None -> index
        }
        (arguments :+ argument) -> newIndex
      }._1)
    }
  }

  /**
   * Creates a response for bound RPC function call result.
   *
   * @param callResult RPC function call result
   * @param rpcRequest RPC request
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return bound function call RPC response
   */
  private def resultResponse[MessageBody: Bytes](
    callResult: Effect[Either[Throwable, (Node, Option[Context])]],
    rpcRequest: RpcRequest[Node, protocol.Metadata],
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] =
    callResult.flatMap { result =>
      result.fold(
        error => logger.error(s"Failed to process ${protocol.name} request", error, requestProperties),
        _ => logger.info(s"Processed ${protocol.name} request", requestProperties)
      )

      // Create response
      Option.when(rpcRequest.responseRequired) {
        response(result.toTry, rpcRequest.message, requestProperties)
      }.getOrElse {
        val responseContext = result.toOption.flatMap(_._2)
        system.pure(HandlerResult(None, None, responseContext))
      }
    }

  /**
   * Creates a handler result containing an RPC response for the specified error.
   *
   * @param error exception
   * @param message RPC message
   * @param responseRequired true if response is required
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return handler result
   */
  private def errorResponse[MessageBody: Bytes](
    error: Throwable,
    message: RpcMessage[protocol.Metadata],
    responseRequired: Boolean,
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] = {
    logger.error(s"Failed to process ${protocol.name} request", error, requestProperties)
    Option.when(responseRequired) {
      response(Failure(error), message, requestProperties)
    }.getOrElse {
      system.pure(HandlerResult(None, None, None))
    }
  }

  /**
   * Creates a handler result containing an RPC response for the specified resul value.
   *
   * @param result a call result on success or an exception on failure
   * @param message RPC message
   * @param requestProperties request properties
   * @tparam MessageBody message body type
   * @return handler result
   */
  private def response[MessageBody: Bytes](
    result: Try[(Node, Option[Context])],
    message: RpcMessage[protocol.Metadata],
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[MessageBody, Context]] =
    protocol.createResponse(result.map(_._1), message.metadata).pureFold(
      error => system.failed(error),
      rpcResponse => {
        val responseBody = rpcResponse.message.body
        lazy val allProperties = rpcResponse.message.properties ++ requestProperties ++
          rpcResponse.message.text.map(LogProperties.messageBody -> _)
        logger.trace(s"Sending ${protocol.name} response", allProperties)
        val responseMessageBody = Some(implicitly[Bytes[MessageBody]].to(responseBody))
        system.pure(HandlerResult(responseMessageBody, result.failed.toOption, result.toOption.flatMap(_._2)))
      }
    )

  private def schemaBindings: ListMap[String, HandlerBinding[Node, Effect, Context]] =
    ListMap(protocol.apiSchemas.map { apiSchema =>
      val describedFunctions = protocol.apiSchemas.filter { apiSchema =>
        !apiBindings.contains(apiSchema.function.name)
      }.map(_.function) ++ apiBindings.values.map(_.function)
      apiSchema.function.name -> HandlerBinding[Node, Effect, Context](
        apiSchema.function,
        Map.empty,
        result => result.asInstanceOf[Node] -> None,
        (_, _) => system.pure(apiSchema.invoke(describedFunctions) -> None),
        acceptsContext = false
      )
    }*)
}

object Handler {

  /** Handler with arbitrary node type. */
  type AnyCodec[Effect[_], Context] = Handler[?, ?, Effect, Context]

  /**
   * Creates an RPC request handler builder with specified RPC protocol plugin.
   *
   * @param protocol RPC protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return RPC request handler builder
   */
  def protocol[Node, Codec <: MessageCodec[Node], Context](
    protocol: RpcProtocol[Node, Codec, Context]
  ): ProtocolHandlerBuilder[Node, Codec, Context] =
    ProtocolHandlerBuilder(protocol)

  /**
   * Creates an RPC request handler builder with specified effect system plugin.
   *
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return RPC request handler builder
   */
  def system[Effect[_]](system: EffectSystem[Effect]): SystemHandlerBuilder[Effect] =
    SystemHandlerBuilder(system)
}
