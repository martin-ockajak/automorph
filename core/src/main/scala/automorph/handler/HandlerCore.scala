package automorph.handler

import automorph.handler.{HandlerMethod, HandlerResult}
import automorph.protocol.ErrorType.{InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}
import automorph.protocol.{ErrorType, Request, Response, ResponseError}
import automorph.spi.{Codec, Message}
import automorph.{Handler, JsonRpcError}
import java.io.IOException
import scala.collection.immutable.ArraySeq
import scala.util.Try

/** JSON-RPC request handler core. */
private[automorph] trait HandlerCore[Node, ExactCodec <: Codec[Node], Effect[_], Context] {
  this: Handler[Node, ExactCodec, Effect, Context] =>

  private val unknownId = "[unknown]"

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @tparam Data message data type
   * @return optional response message
   */
  def processRequest[Data: Bytes](request: Data)(implicit context: Context): Effect[HandlerResult[Data]] = {
    // Deserialize request
    val rawRequest = implicitly[Bytes[Data]].from(request)
    Try(codec.deserialize(rawRequest)).toEither.fold(
      error =>
        errorResponse(
          ParseErrorException("Invalid request format", error),
          Message[Node](None, Some(Right(unknownId)), None, None, None, None)
        ),
      formedRequest => {
        // Validate request
        logger.trace(s"Received JSON-RPC request:\n${codec.format(formedRequest)}")
        Try(Request(formedRequest)).toEither.fold(
          error => errorResponse(error, formedRequest),
          validRequest => invokeMethod(formedRequest, validRequest, context)
        )
      }
    )
  }

  /**
   * Create a copy of this handler with specified exception to JSON-RPC error mapping.
   *
   * @param exceptionToError exception class to JSON-RPC error type mapping
   * @return JSON-RPC server with the specified exceptions to JSON-RPC errors mapping
   */
  def mapExceptions(exceptionToError: Class[_ <: Throwable] => ErrorType): Handler[Node, ExactCodec, Effect, Context] =
    copy(exceptionToError = exceptionToError)

  override def toString: String =
    s"${this.getClass.getName}(Codec: ${codec.getClass.getName}, Backend: ${backend.getClass.getName}, Bound methods: ${methodBindings.size})"

  /**
   * Invoke bound method specified in a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param formedRequest formed request
   * @param validRequest valid request
   * @param context request context
   * @tparam Data message data type
   * @return bound method invocation outcome
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
      Try(backend.either(handlerMethod.invoke(arguments, context))).toEither.fold(
        error => errorResponse(error, formedRequest),
        effect =>
          backend.flatMap(
            effect,
            // Process result
            (outcome: Either[Throwable, Node]) =>
              outcome.fold(
                error => errorResponse(error, formedRequest),
                result => {
                  validRequest.id.foreach(_ => logger.info(s"Processed JSON-RPC request", formedRequest.properties))
                  backend.map(
                    // Serialize response
                    validRequest.id.map { id =>
                      val validResponse = Response(id, Right(result))
                      serialize(validResponse.formed)
                    }.getOrElse(backend.pure(None)),
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
   * Validata and extract specified bound method arguments from a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param validRequest valid request
   * @param handlerMethod handler method binding
   * @return bound method arguments
   */
  private def extractArguments(
    validRequest: Request[Node],
    handlerMethod: HandlerMethod[Node, Effect, Context]
  ): Seq[Node] = {
    // Adjust expected method parameters if it uses context as its last parameter
    val parameters = handlerMethod.paramNames.dropRight(if (handlerMethod.usesContext) 1 else 0)
    validRequest.params.fold(
      arguments =>
        // Arguments by position
        if (arguments.size > parameters.size) {
          throw new IllegalArgumentException(s"Redundant arguments: ${arguments.size - parameters.size}")
        } else {
          arguments ++ Seq.fill(parameters.size - arguments.size)(encodedNone)
        },
      namedArguments => {
        // Arguments by name
        val redundantArguments = namedArguments.keys.toSeq.diff(parameters)
        if (redundantArguments.nonEmpty) {
          throw new IllegalArgumentException(s"Redundant arguments: ${redundantArguments.mkString(", ")}")
        } else {
          parameters.map(name => namedArguments.get(name).getOrElse(encodedNone))
        }
      }
    )
  }

  /**
   * Create an error response for a request.
   *
   * @param error exception
   * @param formedRequest formed request
   * @tparam Data message data type
   * @return error response if applicable
   */
  private def errorResponse[Data: Bytes](error: Throwable, formedRequest: Message[Node]): Effect[HandlerResult[Data]] = {
    logger.error(s"Failed to process JSON-RPC request", error, formedRequest.properties)
    val responseError = error match {
      case JsonRpcError(message, code, data, _) => ResponseError(code, message, data.asInstanceOf[Option[Node]])
      case _ =>
        // Assemble error details
        val code = exceptionToError(error.getClass).code
        val trace = ResponseError.trace(error)
        val message = trace.headOption.getOrElse("Unknown error")
        val data = Some(encodeStrings(trace.drop(1).toList))
        ResponseError(code, message, data)
    }
    backend.map(
      formedRequest.id.map { id =>
        // Serialize response
        val validResponse = Response[Node](id, Left(responseError))
        serialize(validResponse.formed)
      }.getOrElse(backend.pure(None)),
      (rawResponse: Option[ArraySeq.ofByte]) =>
        HandlerResult(rawResponse.map(implicitly[Bytes[Data]].to), formedRequest.id, formedRequest.method, Some(responseError.code))
    )
  }

  /**
   * Serialize JSON-RPC message.
   *
   * @param formedResponse formed response
   * @return serialized response
   */
  private def serialize(formedResponse: Message[Node]): Effect[Option[ArraySeq.ofByte]] = {
    logger.trace(s"Sending JSON-RPC response:\n${codec.format(formedResponse)}")
    Try(codec.serialize(formedResponse)).toEither.fold(
      error => backend.failed(ParseErrorException("Invalid response format", error)),
      message => backend.pure(Some(message))
    )
  }
}

object HandlerCore {

  /**
   * Default exception class to JSON-RPC error type mapping.
   *
   * @param exceptionClass exception class
   * @return JSON-RPC error type
   */
  def defaultMapException(exceptionClass: Class[_ <: Throwable]): ErrorType = Map(
    classOf[ParseErrorException] -> ErrorType.ParseError,
    classOf[InvalidRequestException] -> ErrorType.InvalidRequest,
    classOf[MethodNotFoundException] -> ErrorType.MethodNotFound,
    classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
    classOf[InternalErrorException] -> ErrorType.InternalError,
    classOf[IOException] -> ErrorType.IOError
  ).withDefaultValue(ErrorType.ApplicationError).asInstanceOf[Map[Class[_ <: Throwable], ErrorType]](exceptionClass)
}
