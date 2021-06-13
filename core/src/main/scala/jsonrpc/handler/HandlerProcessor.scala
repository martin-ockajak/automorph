package jsonrpc.handler

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.{Handler, JsonRpcError}
import jsonrpc.protocol.Errors.{MethodNotFound, ParseError}
import jsonrpc.protocol.{Errors, Request, Response, ResponseError}
import jsonrpc.handler.{HandlerMeta, HandlerMethod, HandlerResult}
import jsonrpc.log.Logging
import jsonrpc.spi.{Backend, Codec, Message, MessageError}
import jsonrpc.util.Encoding
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
trait HandlerProcessor[Node, CodecType <: Codec[Node], Effect[_], Context] {
  this: Handler[Node, CodecType, Effect, Context] =>

  private val unknownId = "[unknown]"

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: ArraySeq.ofByte)(using context: Context): Effect[HandlerResult[ArraySeq.ofByte]] =
    // Deserialize request
    Try(codec.deserialize(request)).fold(
      error =>
        errorResponse(
          ParseError("Invalid request format", error),
          Message[Node](None, Some(Right(unknownId)), None, None, None, None)
        ),
      formedRequest =>
        // Validate request
        logger.trace(s"Received JSON-RPC message:\n${codec.format(formedRequest)}")
        Try(Request(formedRequest)).fold(
          error => errorResponse(error, formedRequest),
          validRequest => invokeMethod(formedRequest, validRequest, context)
        )
    )

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: ByteBuffer)(using context: Context): Effect[HandlerResult[ByteBuffer]] =
    backend.map(
      processRequest(Encoding.toArraySeq(request))(using context),
      result => result.copy(response = result.response.map(response => ByteBuffer.wrap(response.unsafeArray)))
    )

  /**
   * Invoke a bound ''method'' based on a JSON-RPC ''request'' and its ''context'' and return a JSON-RPC ''response''.
   *
   * @param request request message
   * @param context request context
   * @return optional response message
   */
  def processRequest(request: InputStream)(using context: Context): Effect[HandlerResult[InputStream]] =
    backend.map(
      processRequest(Encoding.toArraySeq(request, bufferSize))(using context),
      result => result.copy(response = result.response.map(response => ByteArrayInputStream(response.unsafeArray)))
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
      Try(backend.either(handlerMethod.invoke(arguments, context))).fold(
        error => errorResponse(error, formedRequest),
        outcome =>
          backend.flatMap(
            outcome,
            // Process result
            _.fold(
              error => errorResponse(error, formedRequest),
              result =>
                validRequest.id.foreach(_ => logger.info(s"Processed JSON-RPC request", formedRequest.properties))
                backend.map(
                  validRequest.id.map { id =>
                    // Serialize response
                    val validResponse = Response(id, Right(result))
                    serialize(validResponse.formed)
                  }.getOrElse(backend.pure(None)),
                  rawResponse => HandlerResult(rawResponse, formedRequest.id, formedRequest.method, None)
                )
            )
          )
      )
    }.getOrElse {
      errorResponse(
        MethodNotFound(s"Method not found: ${validRequest.method}", None.orNull),
        formedRequest
      )
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
          throw IllegalArgumentException(s"Redundant arguments: ${parameters.size - arguments.size}")
        } else {
          arguments ++ Seq.fill(parameters.size - arguments.size)(encodedNone)
        },
      namedArguments =>
        // Arguments by name
        val redundantArguments = namedArguments.keys.toSeq.diff(parameters)
        if (redundantArguments.nonEmpty) {
          throw IllegalArgumentException(s"Redundant arguments: ${redundantArguments.mkString(", ")}")
        } else {
          parameters.map(name => namedArguments.get(name).getOrElse(encodedNone))
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
    val responseError = error match
      case JsonRpcError(message, code, data, _) => ResponseError(code, message, data.asInstanceOf[Option[Node]])
      case _                                    =>
        // Assemble error details
        val code = Errors.exceptionError(error.getClass).code
        val errorDetails = Errors.errorDetails(error)
        val message = errorDetails.headOption.getOrElse("Unknown error")
        val data = Some(encodeStrings(errorDetails.drop(1)))
        ResponseError(code, message, data)
    backend.map(
      formedRequest.id.map { id =>
        // Serialize response
        val validResponse = Response[Node](id, Left(responseError))
        serialize(validResponse.formed)
      }.getOrElse(backend.pure(None)),
      rawResponse => HandlerResult(rawResponse, formedRequest.id, formedRequest.method, Some(responseError.code))
    )
  }

  /**
   * Serialize JSON-RPC message.
   *
   * @param formedMessage JSON-RPC message
   * @return serialized response
   */
  private def serialize(formedMessage: Message[Node]): Effect[Option[ArraySeq.ofByte]] = {
    logger.trace(s"Sending JSON-RPC message:\n${codec.format(formedMessage)}")
    Try(codec.serialize(formedMessage)).fold(
      error => backend.failed(ParseError("Invalid message format", error)),
      message => backend.pure(Some(message))
    )
  }
}
