package jsonrpc

import jsonrpc.client.ClientMeta
import jsonrpc.log.Logging
import jsonrpc.protocol.{Request, Response}
import jsonrpc.protocol.Errors
import jsonrpc.protocol.Errors.ParseError
import jsonrpc.spi.Message.Params
import jsonrpc.spi.{Backend, Codec, Message, Transport}
import jsonrpc.util.{CannotEqual, Void}
import scala.collection.immutable.ArraySeq
import scala.util.{Random, Try}

/**
 * JSON-RPC client layer.
 *
 * The client can be used by an application to perform JSON-RPC calls and notifications.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins with defined request `Context` type.
 * @param codec message codec plugin
 * @param backend effect backend plugin
 * @param transport message transport plugin
 * @param argumentsByName supply JSON-RPC request arguments ''by name'' if true and ''by position'' if false
 * @tparam Node message format node representation type
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, CodecType <: Codec[Node], Effect[_], Context](
  codec: CodecType,
  backend: Backend[Effect],
  transport: Transport[Effect, Context],
  argumentsByName: Boolean
) extends ClientMeta[Node, CodecType, Effect, Context] with CannotEqual with Logging {

  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  override def toString: String =
    s"${this.getClass.getName}(Codec: ${codec.getClass.getName}, Effect: ${backend.getClass.getName})"

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
  protected def performCall[R](
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
  protected def performNotify(methodName: String, arguments: Params[Node], context: Option[Context]): Effect[Unit] = {
    val formedRequest = Request(None, methodName, arguments).formed
    backend.flatMap(
      // Serialize request
      serialize(formedRequest),
      // Send request
      (rawRequest: ArraySeq.ofByte) => transport.notify(rawRequest, context)
    )
  }

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
    Try(codec.deserialize(rawResponse)).fold(
      error => raiseError(ParseError("Invalid response format", error), formedRequest),
      formedResponse => {
        // Validate response
        logger.trace(s"Received JSON-RPC message:\n${codec.format(formedResponse)}")
        Try(Response(formedResponse)).fold(
          error => raiseError(error, formedRequest),
          validResponse =>
            validResponse.value.fold(
              error => raiseError(Errors.errorException(error.code, error.message), formedRequest),
              result =>
                // Decode result
                Try(decodeResult(result)).fold(
                  error => raiseError(new IllegalStateException("Invalid result", error), formedRequest),
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
   * @param formedMessage JSON-RPC message
   * @return serialized response
   */
  private def serialize(formedMessage: Message[Node]): Effect[ArraySeq.ofByte] = {
    logger.trace(s"Sending JSON-RPC message:\n${codec.format(formedMessage)}")
    Try(codec.serialize(formedMessage)).fold(
      error => raiseError(ParseError("Invalid message format", error), formedMessage),
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

object Client {

  /**
   * Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins with defined request `Context` type.
   *
   * The client can be used by an application to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @param transport message transport plugin
   * @tparam Node message format node representation type
   * @tparam CodecType message codec plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return JSON-RPC request client
   */
  def apply[Node, CodecType <: Codec[Node], Effect[_], Context](
    codec: CodecType,
    backend: Backend[Effect],
    transport: Transport[Effect, Context]
  ): Client[Node, CodecType, Effect, Context] = new Client(codec, backend, transport, true)

  /**
   * Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins without request `Context` type.
   *
   * The client can be used by an application to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @param transport message transport plugin
   * @tparam Node message format node representation type
   * @tparam CodecType message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request client
   */
  def basic[Node, CodecType <: Codec[Node], Effect[_]](
    codec: CodecType,
    backend: Backend[Effect],
    transport: Transport[Effect, Void.Value]
  ): Client[Node, CodecType, Effect, Void.Value] = new Client(codec, backend, transport, true)
}
