package automorph

import automorph.handler.{HandlerBinding, HandlerMeta, HandlerResult, ProtocolHandlerBuilder, SystemHandlerBuilder}
import automorph.log.{LogProperties, Logging}
import automorph.spi.RpcProtocol.FunctionNotFoundException
import automorph.spi.protocol.{RpcFunction, RpcMessage, RpcRequest}
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.Extensions.TryOps
import automorph.util.{Bytes, CannotEqual}
import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

/**
 * RPC request handler.
 *
 * Used by RPC servers to invoke bound API methods based on incoming requests.
 *
 * @constructor Creates a new RPC request handler with specified system and protocol plugins providing corresponding request context type.
 * @param protocol RPC protocol plugin
 * @param system effect system plugin
 * @param bindings API method bindings
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  protocol: RpcProtocol[Node, Codec],
  system: EffectSystem[Effect],
  protected val bindings: ListMap[String, HandlerBinding[Node, Effect, Context]] =
    ListMap.empty[String, HandlerBinding[Node, Effect, Context]]
) extends HandlerMeta[Node, Codec, Effect, Context] with CannotEqual with Logging {

  /** Bound RPC functions. */
  lazy val boundFunctions: Seq[RpcFunction] = bindings.map { case (name, binding) =>
    binding.function.copy(name = name)
  }.toSeq

  /**
   * Processes an RPC ''request'' by invoking a bound RPC ''function'' based on and its ''context'' and return an RPC ''response''.
   *
   * @param requestBody request message body
   * @param requestId request correlation identifier
   * @param context request context
   * @tparam MessageBody message body type
   * @return optional response message
   */
  def processRequest[MessageBody: Bytes](requestBody: MessageBody, requestId: String)(implicit
    context: Context
  ): Effect[HandlerResult[MessageBody]] = {
    // Parse request
    val rawRequest = implicitly[Bytes[MessageBody]].from(requestBody)
    protocol.parseRequest(rawRequest, requestId, None).fold(
      error => errorResponse(error.exception, error.message, requestId, Map(LogProperties.requestId -> requestId)),
      rpcRequest => {
        // Invoke requested RPC function
        lazy val requestProperties = ListMap(LogProperties.requestId -> requestId) ++
          rpcRequest.message.properties + (LogProperties.size -> rawRequest.length.toString)
        lazy val allProperties = requestProperties ++ rpcRequest.message.text.map(LogProperties.body -> _)
        logger.trace(s"Received ${protocol.name} request", allProperties)
        invokeFunction(rpcRequest, context, requestId, requestProperties)
      }
    )
  }

  /**
   * Invokes bound RPC function specified in a request.
   *
   * Optional request context is used as a last RPC function argument.
   *
   * @param rpcRequest RPC request
   * @param context request context
   * @param requestId request correlation idendifier
   * @param requestProperties request properties
   * @tparam Body message body type
   * @return bound function invocation result
   */
  private def invokeFunction[Body: Bytes](
    rpcRequest: RpcRequest[Node, protocol.Metadata],
    context: Context,
    requestId: String,
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[Body]] = {
    // Lookup bindings for the specified RPC function
    logger.debug(s"Processing ${protocol.name} request", requestProperties)
    bindings.get(rpcRequest.function).map { handlerBinding =>
      // Extract arguments
      extractArguments(rpcRequest, handlerBinding).flatMap { arguments =>
        // Invoke bound function
        Try(system.either(handlerBinding.invoke(arguments, context)))
      }.pureFold(
        error => errorResponse(error, rpcRequest.message, requestId, requestProperties),
        effect => {
          logger.info(s"Processed ${protocol.name} request", requestProperties)
          system.flatMap(
            effect,
            (outcome: Either[Throwable, Node]) =>
              if (rpcRequest.responseRequired) {
                // Create response
                response(outcome.toTry, rpcRequest.message, requestId)
              } else {
                system.pure(HandlerResult[Body](None, None))
              }
          )
        }
      )
    }.getOrElse {
      val error = FunctionNotFoundException(s"Function not found: ${rpcRequest.function}", None.orNull)
      errorResponse(error, rpcRequest.message, requestId, requestProperties)
    }
  }

  /**
   * Validates and extracts specified bound RPC function arguments from a request.
   *
   * Optional request context is used as a last RPC function argument.
   *
   * @param rpcRequest RPC request
   * @param handlerBinding handler RPC function binding
   * @return bound function arguments
   */
  private def extractArguments(
    rpcRequest: RpcRequest[Node, _],
    handlerBinding: HandlerBinding[Node, Effect, Context]
  ): Try[Seq[Option[Node]]] = {
    // Adjust expected function parameters if it uses context as its last parameter
    val parameters = handlerBinding.function.parameters
    val parameterNames = parameters.map(_.name).dropRight(if (handlerBinding.usesContext) 1 else 0)
    rpcRequest.arguments.fold(
      positionalArguments => {
        // Arguments by position
        val redundantSize = positionalArguments.size - parameterNames.size
        if (redundantSize > 0) {
          Failure(new IllegalArgumentException(
            s"Redundant arguments: ${Range(parameterNames.size, redundantSize).mkString(", ")}"
          ))
        } else {
          Success(positionalArguments.map(Some(_)) ++ Seq.fill(-redundantSize)(None))
        }
      },
      namedArguments => {
        // Arguments by name
        val redundantNames = namedArguments.keys.toSeq.diff(parameterNames)
        if (redundantNames.nonEmpty) {
          Failure(new IllegalArgumentException(s"Redundant arguments: ${redundantNames.mkString(", ")}"))
        } else {
          Success(parameterNames.map(namedArguments.get))
        }
      }
    )
  }

  /**
   * Creates a handler result containing an RPC response for the specified error.
   *
   * @param error exception
   * @param message RPC message
   * @param requestId request correlation idendifier
   * @param requestProperties request properties
   * @tparam Body message body type
   * @return handler result
   */
  private def errorResponse[Body: Bytes](
    error: Throwable,
    message: RpcMessage[protocol.Metadata],
    requestId: String,
    requestProperties: => Map[String, String]
  ): Effect[HandlerResult[Body]] = {
    logger.error(s"Failed to process ${protocol.name} request", error, requestProperties)
    response(Failure(error), message, requestId)
  }

  /**
   * Creates a handler result containing an RPC response for the specified resul value.
   *
   * @param result a call result on success or an exception on failure
   * @param message RPC message
   * @param requestId request correlation idendifier
   * @tparam Body message body type
   * @return handler result
   */
  private def response[Body: Bytes](
    result: Try[Node],
    message: RpcMessage[protocol.Metadata],
    requestId: String
  ): Effect[HandlerResult[Body]] =
    protocol.createResponse(result, message.details).pureFold(
      error => system.failed(error),
      rpcResponse => {
        val rawResponse = rpcResponse.message.body
        lazy val requestProperties = rpcResponse.message.properties ++ Map(
          LogProperties.requestId -> requestId,
          LogProperties.size -> rawResponse.length.toString
        )
        lazy val allProperties = requestProperties ++ rpcResponse.message.text.map(LogProperties.body -> _)
        logger.trace(s"Sending ${protocol.name} response", allProperties)
        system.pure(HandlerResult(Some(implicitly[Bytes[Body]].to(rawResponse)), result.failed.toOption))
      }
    )

  override def toString: String = {
    val plugins = Map(
      "system" -> system,
      "protocol" -> protocol
    ).map { case (name, plugin) => s"$name = ${plugin.getClass.getName}" }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }
}

object Handler {

  /** Handler with arbitrary node type. */
  type AnyCodec[Effect[_], Context] = Handler[_, _, Effect, Context]

  /**
   * Creates an RPC request handler builder with specified RPC protocol plugin.
   *
   * @param protocol RPC protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return RPC request handler builder
   */
  def protocol[Node, Codec <: MessageCodec[Node]](
    protocol: RpcProtocol[Node, Codec]
  ): ProtocolHandlerBuilder[Node, Codec] =
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
