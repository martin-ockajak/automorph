package jsonrpc

import java.io.IOException
import jsonrpc.Client.defaultMapError
import jsonrpc.client.ClientMeta
import jsonrpc.log.Logging
import jsonrpc.protocol.ErrorType.{InternalErrorException, InvalidRequestException, InvalidResponseException, MethodNotFoundException, ParseErrorException}
import jsonrpc.protocol.{ErrorType, Request, Response}
import jsonrpc.spi.Message.Params
import jsonrpc.spi.{Backend, Codec, Message, Transport}
import jsonrpc.util.{CannotEqual, NoContext}
import scala.collection.immutable.ArraySeq
import scala.util.{Random, Try}

/**
 * JSON-RPC client.
 *
 * The client can be used to perform JSON-RPC calls and notifications.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins with defined request `Context` type.
 * @param codec message codec plugin
 * @param backend effect backend plugin
 * @param transport message transport plugin
 * @param mapError mapping of JSON-RPC errors to exceptions
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, ExactCodec <: Codec[Node], Effect[_], Context] (
  codec: ExactCodec,
  backend: Backend[Effect],
  transport: Transport[Effect, Context],
  mapError: (Int, String) => Throwable = defaultMapError
) extends ClientMeta[Node, ExactCodec, Effect, Context] with CannotEqual with Logging {

  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  /**
   * Perform a method call using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param arguments method arguments
   * @param context request context
   * @param decodeResult result decoding function
   * @tparam R result type
   * @return result value
   */
  def performCall[R](
    method: String,
    arguments: Params[Node],
    context: Option[Context],
    decodeResult: Node => R
  ): Effect[R] = {
    val id = Some(Right[BigDecimal, String](Math.abs(random.nextLong()).toString))
    val formedRequest = Request(id, method, arguments).formed
    logger.debug(s"Performing JSON-RPC request", formedRequest.properties)
    backend.flatMap(
      // Serialize request
      serialize(formedRequest),
      (rawRequest: ArraySeq.ofByte) =>
        // Send request
        backend.flatMap(
          transport.call(rawRequest, context),
          // Process response
          rawResponse => processResponse[R](rawResponse, formedRequest, decodeResult)
        )
    )
  }

  /**
   * Perform a method notification using specified arguments.
   *
   * Optional request context is used as a last method argument.
   *
   * @param methodName method name
   * @param arguments method arguments
   * @param context request context
   * @tparam R result type
   * @return nothing
   */
  def performNotify(methodName: String, arguments: Params[Node], context: Option[Context]): Effect[Unit] = {
    val formedRequest = Request(None, methodName, arguments).formed
    backend.flatMap(
      // Serialize request
      serialize(formedRequest),
      // Send request
      (rawRequest: ArraySeq.ofByte) => transport.notify(rawRequest, context)
    )
  }

  override def toString: String =
    s"${this.getClass.getName}(Codec: ${codec.getClass.getName}, Backend: ${backend.getClass.getName}, Transport: ${transport.getClass.getName})"

  /**
   * Process a method call response.
   *
   * @param rawResponse raw response
   * @param formedRequest formed request
   * @param decodeResult result decoding function
   * @tparam R result type
   * @return result value
   */
  private def processResponse[R](
    rawResponse: ArraySeq.ofByte,
    formedRequest: Message[Node],
    decodeResult: Node => R
  ): Effect[R] =
    // Deserialize response
    Try(codec.deserialize(rawResponse)).toEither.fold(
      error => raiseError(ParseErrorException("Invalid response format", error), formedRequest) ,
      formedResponse => {
        // Validate response
        logger.trace(s"Received JSON-RPC response:\n${codec.format(formedResponse)}")
        Try(Response(formedResponse)).toEither.fold(
          error => raiseError(error, formedRequest),
          validResponse =>
            validResponse.value.fold(
              error => raiseError(mapError(error.code, error.message), formedRequest),
              result =>
                // Decode result
                Try(decodeResult(result)).toEither.fold(
                  error => raiseError(InvalidResponseException("Invalid result", error), formedRequest),
                  result => {
                    logger.info(s"Performed JSON-RPC request", formedRequest.properties)
                    backend.pure(result)
                  }
                )
            )
        )
      }
    )

  /**
   * Serialize JSON-RPC message.
   *
   * @param formedRequest formed request
   * @return serialized response
   */
  private def serialize(formedRequest: Message[Node]): Effect[ArraySeq.ofByte] = {
    logger.trace(s"Sending JSON-RPC request:\n${codec.format(formedRequest)}")
    Try(codec.serialize(formedRequest)).toEither.fold(
      error => raiseError(ParseErrorException("Invalid request format", error), formedRequest),
      message => backend.pure(message)
    )
  }

  /**
   * Create an error effect from an exception.
   *
   * @param error exception
   * @param requestMessage request message
   * @tparam T effectful value type
   * @return error value
   */
  private def raiseError[T](error: Throwable, requestMessage: Message[Node]): Effect[T] = {
    logger.error(s"Failed to perform JSON-RPC request", error, requestMessage.properties)
    backend.failed(error)
  }
}

case object Client {

  /**
   * Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins with empty request `Context` type.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @param transport message transport plugin
   * @param mapError mapping of JSON-RPC errors to exceptions
   * @tparam Node message format node representation type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC client
   */
  def noContext[Node, ExactCodec <: Codec[Node], Effect[_]](
    codec: ExactCodec,
    backend: Backend[Effect],
    transport: Transport[Effect, NoContext.Value],
    mapError: (Int, String) => Throwable = defaultMapError
  ): Client[Node, ExactCodec, Effect, NoContext.Value] = new Client(codec, backend, transport, mapError)

  /**
   * Default mapping of JSON-RPC errors to exceptions.
   *
   * @param code error code
   * @param message error message
   * @return exception
   */
  def defaultMapError(code: Int, message: String): Throwable = code match {
    case ErrorType.ParseError.code => ParseErrorException(message, None.orNull)
    case ErrorType.InvalidRequest.code => InvalidRequestException(message, None.orNull)
    case ErrorType.MethodNotFound.code => MethodNotFoundException(message, None.orNull)
    case ErrorType.InvalidParams.code => new IllegalArgumentException(message, None.orNull)
    case ErrorType.InternalError.code => InternalErrorException(message, None.orNull)
    case ErrorType.IOError.code => new IOException(message, None.orNull)
    case _ if code < ErrorType.ApplicationError.code => InternalErrorException(message, None.orNull)
    case _ => new RuntimeException(message, None.orNull)
  }
}
