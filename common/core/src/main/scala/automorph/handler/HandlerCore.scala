package automorph.handler

import automorph.Handler
import automorph.spi.RpcProtocol.FunctionNotFoundException
import automorph.spi.protocol.{RpcFunction, RpcMessage, RpcRequest}
import automorph.spi.{MessageCodec, RpcProtocol}
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import scala.util.{Failure, Success, Try}

/**
 * RPC request handler core logic.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait HandlerCore[Node, Codec <: MessageCodec[Node], Effect[_], Context] {
  this: Handler[Node, Codec, Effect, Context] =>

  private val bodyProperty = "Body"

  /** Bound RPC functions. */
  lazy val boundFunctions: Seq[RpcFunction] = bindings.map { case (name, binding) =>
    binding.function.copy(name = name)
  }.toSeq

  /**
   * Processes an RPC ''request'' by invoking a bound ''method'' based on and its ''context'' and return an RPC ''response''.
   *
   * @param request request message
   * @param requestId request correlation identifier
   * @param context request context
   * @tparam Body message body type
   * @return optional response message
   */
  def processRequest[Body: Bytes](request: Body, requestId: String)(implicit context: Context): Effect[HandlerResult[Body]] = {
    // Parse request
    val rawRequest = implicitly[Bytes[Body]].from(request)
    protocol.parseRequest(rawRequest, None).fold(
      error => errorResponse(error.exception, error.message),
      rpcRequest => {
        lazy val properties = rpcRequest.message.properties ++ rpcRequest.message.text.map(bodyProperty -> _)
        logger.trace(s"Received ${protocol.name} request", properties)
        invokeMethod(rpcRequest, context)
      }
    )
  }

  override def toString: String = {
    val plugins = Map(
      "system" -> system,
      "protocol" -> protocol
    ).map { case (name, plugin) => s"$name = ${plugin.getClass.getName}" }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }

  /**
   * Invokes bound method specified in a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param rpcRequest RPC request
   * @param context request context
   * @tparam Body message body type
   * @return bound method invocation result
   */
  private def invokeMethod[Body: Bytes](
    rpcRequest: RpcRequest[Node, protocol.Metadata],
    context: Context
  ): Effect[HandlerResult[Body]] = {
    // Lookup bindings for the specified method
    logger.debug(s"Processing ${protocol.name} request", rpcRequest.message.properties)
    bindings.get(rpcRequest.function).map { handlerBinding =>
      // Extract arguments
      extractArguments(rpcRequest, handlerBinding).flatMap { arguments =>
        // Invoke method
        Try(system.either(handlerBinding.invoke(arguments, context)))
      }.pureFold(
        error => errorResponse(error, rpcRequest.message),
        effect => {
          logger.info(s"Processed ${protocol.name} request", rpcRequest.message.properties)
          system.flatMap(
            effect,
            (outcome: Either[Throwable, Node]) =>
              if (rpcRequest.responseRequired) {
                // Create response
                response(outcome.toTry, rpcRequest.message)
              } else {
                system.pure(HandlerResult[Body](None, None))
              }
          )
        }
      )
    }.getOrElse {
      val error = FunctionNotFoundException(s"Method not found: ${rpcRequest.function}", None.orNull)
      errorResponse(error, rpcRequest.message)
    }
  }

  /**
   * Validates and extracts specified bound method arguments from a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param rpcRequest RPC request
   * @param handlerMethod handler method binding
   * @return bound method arguments
   */
  private def extractArguments(
    rpcRequest: RpcRequest[Node, _],
    handlerMethod: HandlerBinding[Node, Effect, Context]
  ): Try[Seq[Option[Node]]] = {
    // Adjust expected method parameters if it uses context as its last parameter
    val parameters = handlerMethod.function.parameters
    val parameterNames = parameters.map(_.name).dropRight(if (handlerMethod.usesContext) 1 else 0)
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
   * @tparam Body message body type
   * @return handler result
   */
  private def errorResponse[Body: Bytes](
    error: Throwable,
    message: RpcMessage[protocol.Metadata]
  ): Effect[HandlerResult[Body]] = {
    logger.error(s"Failed to process ${protocol.name} request", error, message.properties)
    response(Failure(error), message)
  }

  /**
   * Creates a handler result containing an RPC response for the specified resul value.
   *
   * @param result a call result on success or an exception on failure
   * @param message RPC message
   * @tparam Body message body type
   * @return handler result
   */
  private def response[Body: Bytes](
    result: Try[Node],
    message: RpcMessage[protocol.Metadata]
  ): Effect[HandlerResult[Body]] =
    protocol.createResponse(result, message.details).pureFold(
      error => system.failed(error),
      rpcResponse => {
        lazy val properties = rpcResponse.message.properties ++ rpcResponse.message.text.map(bodyProperty -> _)
        logger.trace(s"Sending ${protocol.name} response", properties)
        system.pure(HandlerResult(Some(implicitly[Bytes[Body]].to(rpcResponse.message.body)), result.failed.toOption))
      }
    )
}
