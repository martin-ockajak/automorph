package jsonrpc.handler

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.Protocol.{MethodNotFound, ParseError}
import jsonrpc.core.{Empty, Protocol, Request, Response, ResponseError}
import jsonrpc.handler.{HandlerMeta, HandlerResult, MethodHandle}
import jsonrpc.log.Logging
import jsonrpc.spi.{Backend, Codec, Message, MessageError}
import jsonrpc.util.CannotEqual
import jsonrpc.util.EncodingOps.toArraySeq
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome, className}
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
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, CodecType <: Codec[Node], Effect[_], Context](
  codec: CodecType,
  backend: Backend[Effect],
  bufferSize: Int,
  protected val methodBindings: Map[String, MethodHandle[Node, Effect, Context]],
  protected val encodeStrings: Seq[String] => Node
) extends HandlerMeta[Node, CodecType, Effect, Context] with CannotEqual with Logging:

  private val unknownId = "[unknown]".asRight

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
          Message[Node](None, unknownId.asSome, None, None, None, None)
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
      processRequest(request.toArraySeq)(using context),
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
      processRequest(request.toArraySeq(bufferSize))(using context),
      result => result.copy(response = result.response.map(response => ByteArrayInputStream(response.unsafeArray)))
    )

  override def toString: String =
    s"${this.className}(Codec: ${codec.className}, Effect: ${backend.className}, Bound methods: ${methodBindings.size})"

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
  ): Effect[HandlerResult[ArraySeq.ofByte]] =
    logger.debug(s"Processing JSON-RPC request", formedRequest.properties)
    methodBindings.get(validRequest.method).map { methodHandle =>
      // Extract arguments
      val arguments = extractArguments(validRequest, methodHandle)

      // Invoke method
      Try(backend.either(methodHandle.function(arguments, context))).fold(
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
                    val validResponse = Response(id, result.asRight)
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

  /**
   * Validata and extract specified bound method arguments from a request.
   *
   * Optional request context is used as a last method argument.
   *
   * @param validRequest valid request
   * @param methodHandle bound method handle
   * @return bound method arguments
   */
  private def extractArguments(
    validRequest: Request[Node],
    methodHandle: MethodHandle[Node, Effect, Context]
  ): Seq[Node] =
    val parameters = methodHandle.paramNames.dropRight(if methodHandle.usesContext then 1 else 0)
    validRequest.params.fold(
      arguments =>
        // Arguments by position
        if arguments.size < parameters.size then
          throw IllegalArgumentException(s"Missing arguments: ${parameters.drop(arguments.size)}")
        else if arguments.size > parameters.size then
          throw IllegalArgumentException(s"Redundant arguments: ${parameters.size - arguments.size}")
        arguments
      ,
      namedArguments =>
        // Arguments by name
        val arguments = parameters.flatMap(namedArguments.get)
        if arguments.size < parameters.size then
          throw IllegalArgumentException(s"Missing arguments: ${parameters.filterNot(namedArguments.contains)}")
        else if arguments.size > parameters.size then
          throw IllegalArgumentException(s"Redundant arguments: ${namedArguments.keys.filterNot(parameters.contains)}")
        arguments
    )

  /**
   * Create an error response for a request.
   *
   * @param error exception
   * @param formedRequest formed request
   * @return error response if applicable
   */
  private def errorResponse(error: Throwable, formedRequest: Message[Node]): Effect[HandlerResult[ArraySeq.ofByte]] =
    logger.error(s"Failed to process JSON-RPC request", error, formedRequest.properties)
    val code = Protocol.exceptionError(error.getClass).code
    backend.map(
      formedRequest.id.map { id =>
        // Assemble error details
        val errorDetails = Protocol.errorDetails(error)
        val message = errorDetails.headOption.getOrElse("Unknown error")
        val data = encodeStrings(errorDetails.drop(1)).asSome

        // Serialize response
        val validResponse = Response[Node](id, ResponseError(code, message, data).asLeft)
        serialize(validResponse.formed)
      }.getOrElse(backend.pure(None)),
      rawResponse => HandlerResult(rawResponse, formedRequest.id, formedRequest.method, code.asSome)
    )

  /**
   * Serialize JSON-RPC message.
   *
   * @param formedMessage JSON-RPC message
   * @return serialized response
   */
  private def serialize(formedMessage: Message[Node]): Effect[Option[ArraySeq.ofByte]] =
    logger.trace(s"Sending JSON-RPC message:\n${codec.format(formedMessage)}")
    Try(codec.serialize(formedMessage)).fold(
      error => backend.failed(ParseError("Invalid message format", error)),
      message => backend.pure(message.asSome)
    )
