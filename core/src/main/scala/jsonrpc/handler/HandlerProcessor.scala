package jsonrpc.handler

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.ByteBuffer
import jsonrpc.handler.{HandlerMethod, HandlerResult}
import jsonrpc.protocol.ErrorType.{MethodNotFoundException, ParseErrorException}
import jsonrpc.protocol.{ErrorType, Request, Response, ResponseError}
import jsonrpc.spi.{Codec, Message}
import jsonrpc.util.Encoding
import jsonrpc.{Handler, JsonRpcError}
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * JSON-RPC request handler layer.
 *
 * The handler can be used by a JSON-RPC server to process incoming JSON-RPC requests, invoke the requested API methods and return JSON-RPC responses.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request `Context` type.
 * @param codec message codec plugin
 * @param backend effect backend plugin
 * @param bufferSize input stream reading buffer size
 * @tparam Node message format node representation type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
private[jsonrpc] trait HandlerProcessor[Node, ExactCodec <: Codec[Node], Effect[_], Context] {
  this: Handler[Node, ExactCodec, Effect, Context] =>

  private val unknownId = "[unknown]"

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: ArraySeq.ofByte)(implicit context: Context): Effect[HandlerResult[ArraySeq.ofByte]] =
    // Deserialize request
    Try(codec.deserialize(request)).toEither.fold(
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

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: ByteBuffer)(implicit context: Context): Effect[HandlerResult[ByteBuffer]] =
    backend.map(
      processRequest(Encoding.toArraySeq(request))(context),
      (result: HandlerResult[ArraySeq.ofByte]) =>
        result.copy(response = result.response.map(response => ByteBuffer.wrap(response.unsafeArray)))
    )

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: InputStream)(implicit context: Context): Effect[HandlerResult[InputStream]] =
    backend.map(
      processRequest(Encoding.toArraySeq(request, bufferSize))(context),
      (result: HandlerResult[ArraySeq.ofByte]) =>
        result.copy(response = result.response.map(response => new ByteArrayInputStream(response.unsafeArray)))
    )

  override def toString: String =
    s"${this.getClass.getName}(Codec: ${codec.getClass.getName}, Effect: ${backend.getClass.getName}, Bound methods: ${methodBindings.size})"

  /**
   * Invoke bound method specified in a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param formedRequest formed request
   * @param validRequest valid request
   * @param context request context
   * @return bound method invocation outcome
   */
  private def invokeMethod(
    formedRequest: Message[Node],
    validRequest: Request[Node],
    context: Context
  ): Effect[HandlerResult[ArraySeq.ofByte]] = {
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
                      HandlerResult(rawResponse, formedRequest.id, formedRequest.method, None)
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
   * @return error response if applicable
   */
  private def errorResponse(error: Throwable, formedRequest: Message[Node]): Effect[HandlerResult[ArraySeq.ofByte]] = {
    logger.error(s"Failed to process JSON-RPC request", error, formedRequest.properties)
    val responseError = error match {
      case JsonRpcError(message, code, data, _) => ResponseError(code, message, data.asInstanceOf[Option[Node]])
      case _                                    =>
        // Assemble error details
        val code = ErrorType.fromException(error.getClass).code
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
        HandlerResult(rawResponse, formedRequest.id, formedRequest.method, Some(responseError.code))
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
