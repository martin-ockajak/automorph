package automorph.handler

import automorph.protocol.jsonrpc.ErrorType.{InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}
import automorph.protocol.jsonrpc.{ErrorType, Request, Response, ResponseError}
import automorph.spi.{Message, MessageFormat}
import automorph.util.Bytes
import automorph.util.Extensions.TryOps
import automorph.{Handler, JsonRpcError}
import java.io.IOException
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * JSON-RPC request handler core logic.
 *
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[automorph] trait HandlerCore[Node, Format <: MessageFormat[Node], Effect[_], Context] {
  this: Handler[Node, Format, Effect, Context] =>

  private val unknownId = "[unknown]"

  /**
   * Processes a JSON-RPC ''request'' by invoking a bound ''method'' based on and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @tparam Data message data type
   * @return optional response message
   */
  def processRequest[Data: Bytes](request: Data)(implicit context: Context): Effect[HandlerResult[Data]] = {
    // Deserialize request
    val rawRequest = implicitly[Bytes[Data]].from(request)
    Try(format.deserialize(rawRequest)).pureFold(
      error =>
        errorResponse(
          ParseErrorException("Invalid request format", error),
          Message[Node](None, Some(Right(unknownId)), None, None, None, None)
        ),
      formedRequest => {
        // Validate request
        logger.trace(s"Received JSON-RPC request:\n${format.format(formedRequest)}")
        Try(Request(formedRequest)).pureFold(
          error => errorResponse(error, formedRequest),
          validRequest => invokeMethod(formedRequest, validRequest, context)
        )
      }
    )
  }

  /**
   * Creates a copy of this handler with specified exception to JSON-RPC error mapping.
   *
   * @param exceptionToError maps an exception classs to a corresponding JSON-RPC error type
   * @return JSON-RPC request handler
   */
  def errorMapping(exceptionToError: Throwable => ErrorType): ThisHandler =
    copy(exceptionToError = exceptionToError)

  override def toString: String =
    s"${this.getClass.getName}(format = ${format.getClass.getName}, system = ${system.getClass.getName})"

  /**
   * Invokes bound method specified in a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param formedRequest formed request
   * @param validRequest valid request
   * @param context request context
   * @tparam Data message data type
   * @return bound method invocation result
   */
  private def invokeMethod[Data: Bytes](
    formedRequest: Message[Node],
    validRequest: Request[Node],
    context: Context
  ): Effect[HandlerResult[Data]] = {
    // Lookup bindings for the specified method
    logger.debug(s"Processing JSON-RPC request", formedRequest.properties)
    methodBindings.get(validRequest.method).map { handlerMethod =>
      // Extract arguments
      val arguments = extractArguments(validRequest, handlerMethod)

      // Invoke method
      Try(system.either(handlerMethod.invoke(arguments, context))).pureFold(
        error => errorResponse(error, formedRequest),
        effect =>
          system.flatMap(
            effect,
            (outcome: Either[Throwable, Node]) =>
              // Process result
              outcome.fold(
                error => errorResponse(error, formedRequest),
                result => {
                  validRequest.id.foreach(_ => logger.info(s"Processed JSON-RPC request", formedRequest.properties))
                  system.map(
                    // Serialize response
                    validRequest.id.map { id =>
                      val validResponse = Response(id, Some(result), None)
                      serialize(validResponse.formed)
                    }.getOrElse(system.pure(None)),
                    (rawResponse: Option[ArraySeq.ofByte]) =>
                      HandlerResult(rawResponse.map(implicitly[Bytes[Data]].to), formedRequest.id, formedRequest.method, None)
                  )
                }
              )
          )
      )
    }.getOrElse {
      errorResponse(MethodNotFoundException(s"Method not found: ${validRequest.method}", None.orNull), formedRequest)
    }
  }

  /**
   * Validates and extracts specified bound method arguments from a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param validRequest valid request
   * @param handlerMethod handler method binding
   * @return bound method arguments
   */
  private def extractArguments(
    validRequest: Request[Node],
    handlerMethod: HandlerBinding[Node, Effect, Context]
  ): Seq[Node] = {
    // Adjust expected method parameters if it uses context as its last parameter
    val parameters = handlerMethod.method.parameters.flatten
    val parameterNames = parameters.map(_.name).dropRight(if (handlerMethod.usesContext) 1 else 0)
    validRequest.params.fold(
      arguments =>
        // Arguments by position
        if (arguments.size > parameterNames.size) {
          throw new IllegalArgumentException(s"Redundant arguments: ${arguments.size - parameterNames.size}")
        } else {
          arguments ++ Seq.fill(parameterNames.size - arguments.size)(encodedNone)
        },
      namedArguments => {
        // Arguments by name
        val redundantArguments = namedArguments.keys.toSeq.diff(parameterNames)
        if (redundantArguments.nonEmpty) {
          throw new IllegalArgumentException(s"Redundant arguments: ${redundantArguments.mkString(", ")}")
        } else {
          parameterNames.map(name => namedArguments.getOrElse(name, encodedNone))
        }
      }
    )
  }

  /**
   * Creates an error response for a request.
   *
   * @param error exception
   * @param formedRequest formed request
   * @tparam Data message data type
   * @return error response if applicable
   */
  private def errorResponse[Data: Bytes](error: Throwable, formedRequest: Message[Node]): Effect[HandlerResult[Data]] = {
    logger.error(s"Failed to process JSON-RPC request", error, formedRequest.properties)
    val responseError = error match {
      case JsonRpcError(message, code, data, _) => ResponseError(message, code, data.asInstanceOf[Option[Node]])
      case _ =>
        // Assemble error details
        val trace = ResponseError.trace(error)
        val message = trace.headOption.getOrElse("Unknown error")
        val code = exceptionToError(error).code
        val data = Some(encodeStrings(trace.drop(1).toList))
        ResponseError(message, code, data)
    }
    system.map(
      formedRequest.id.map { id =>
        // Serialize response
        val validResponse = Response[Node](id, None, Some(responseError))
        serialize(validResponse.formed)
      }.getOrElse(system.pure(None)),
      (rawResponse: Option[ArraySeq.ofByte]) =>
        HandlerResult(rawResponse.map(implicitly[Bytes[Data]].to), formedRequest.id, formedRequest.method, Some(responseError.code))
    )
  }

  /**
   * Serializes a JSON-RPC message.
   *
   * @param formedResponse formed response
   * @return serialized response
   */
  private def serialize(formedResponse: Message[Node]): Effect[Option[ArraySeq.ofByte]] = {
    logger.trace(s"Sending JSON-RPC response:\n${format.format(formedResponse)}")
    Try(format.serialize(formedResponse)).pureFold(
      error => system.failed(ParseErrorException("Invalid response format", error)),
      message => system.pure(Some(message))
    )
  }
}

private[automorph] object HandlerCore {

  /**
   * Maps an exception class to a corresponding default JSON-RPC error type.
   *
   * @param exception exception class
   * @return JSON-RPC error type
   */
  def defaultErrorMapping(exception: Throwable): ErrorType = exception match {
    case _: ParseErrorException => ErrorType.ParseError
    case _: InvalidRequestException => ErrorType.InvalidRequest
    case _: MethodNotFoundException => ErrorType.MethodNotFound
    case _: IllegalArgumentException => ErrorType.InvalidParams
    case _: InternalErrorException => ErrorType.InternalError
    case _: IOException => ErrorType.IOError
    case _ => ErrorType.ApplicationError
  }
}
