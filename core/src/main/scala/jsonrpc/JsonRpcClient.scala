package jsonrpc

import jsonrpc.client.ClientMacros
import jsonrpc.core.Protocol.{MethodNotFoundException, ParseErrorException}
import jsonrpc.core.{Empty, Protocol, Request, Response, ResponseError}
import jsonrpc.log.Logging
import jsonrpc.spi.Message.Params
import jsonrpc.spi.{Codec, Backend, Message, MessageError, Transport}
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome}
import jsonrpc.util.CannotEqual
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
 * @tparam Node message format node representation type
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class JsonRpcClient[Node, CodecType <: Codec[Node], Effect[_], Context](
  codec: CodecType,
  backend: Backend[Effect],
  transport: Transport[Effect, Context]
) extends CannotEqual with Logging:

  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the ''arguments by name''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by by name
   * @param context request context
   * @tparam R result type
   * @return result value
   */
  inline def call[A <: Product, R](method: String, arguments: A)(using context: Context): Effect[R] =
    performCall(method, encodeArguments(arguments), context, decodeResult[R])

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by name''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by name
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return nothing
   */
  inline def notify[A <: Product](method: String, arguments: A)(using context: Context): Effect[Unit] =
    performNotify(method, encodeArguments(arguments), context)

  /**
   * Create a transparent ''proxy instance'' of a remote JSON-RPC API.
   *
   * Invocations of local proxy methods are translated into remote JSON-API calls.
   *
   * @tparam T remote API type
   * @return remote API proxy instance
   */
  inline def bind[T <: AnyRef]: T = ClientMacros.bind[Node, CodecType, Effect, Context, T](codec, backend)

  /**
   * Encode request arguments by name.
   *
   * @param arguments request arguments
   * @return encoded request arguments
   */
  inline def encodeArguments[A <: Product](arguments: A): Params[Node] =
    val argumentsNode = codec.encode(arguments)
    codec.decode[Map[String, Node]](argumentsNode).asRight

  /**
   * Create response result decoding function.
   *
   * @tparam R result type
   * @return result decoding function
   */
  inline def decodeResult[R]: Node => R =
    resultNode => codec.decode(resultNode)

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
  private def performCall[R](
    method: String,
    arguments: Params[Node],
    context: Context,
    decodeResult: Node => R
  ): Effect[R] =
    val id = Math.abs(random.nextLong()).toString.asRight[BigDecimal].asSome
    val formedRequest = Request(id, method, arguments).formed
    logger.debug(s"Performing JSON-RPC request", formedRequest.properties)
    backend.flatMap(
      // Serialize request
      serialize(formedRequest),
      rawRequest =>
        // Send request
        backend.flatMap(
          transport.call(rawRequest, context),
          // Process response
          rawResponse => processResponse[R](rawResponse, formedRequest, decodeResult)
        )
    )

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
  private def performNotify(methodName: String, arguments: Params[Node], context: Context): Effect[Unit] =
    val formedRequest = Request(None, methodName, arguments).formed
    backend.map(
      // Serialize request
      serialize(formedRequest),
      rawRequest =>
        // Send request
        transport.notify(rawRequest, context)
    )

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
      error => raiseError(ParseErrorException("Invalid response format", error), formedRequest),
      formedResponse =>
        // Validate response
        logger.trace(s"Received JSON-RPC message:\n${codec.format(formedResponse)}")
        Try(Response(formedResponse)).fold(
          error => raiseError(error, formedRequest),
          validResponse =>
            validResponse.value.fold(
              error => raiseError(Protocol.errorException(error.code, error.message), formedRequest),
              result =>
                // Decode result
                Try(decodeResult(result)).fold(
                  error => raiseError(error, formedRequest),
                  result =>
                    logger.info(s"Performed JSON-RPC request", formedRequest.properties)
                    backend.pure(result)
                )
            )
        )
    )

  /**
   * Serialize JSON-RPC message.
   *
   * @param formedMessage JSON-RPC message
   * @return serialized response
   */
  private def serialize(formedMessage: Message[Node]): Effect[ArraySeq.ofByte] =
    logger.trace(s"Sending JSON-RPC message:\n${codec.format(formedMessage)}")
    Try(codec.serialize(formedMessage)).fold(
      error => raiseError(ParseErrorException("Invalid message format", error), formedMessage),
      message => backend.pure(message)
    )

  /**
   * Create an error effect from an exception.
   *
   * @param error exception
   * @param requestMessage request message
   * @tparam T effectful value type
   * @return error value
   */
  private def raiseError[T](error: Throwable, requestMessage: Message[Node]): Effect[T] =
    logger.error(s"Failed to perform JSON-RPC request", error, requestMessage.properties)
    backend.failed(error)

case object JsonRpcClient:

  type NoContext = Empty[JsonRpcClient[?, ?, ?, ?]]
  given NoContext = Empty[JsonRpcClient[?, ?, ?, ?]]()

  /**
   * Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins without request `Context` type.
   *
   * The client can be used by an application to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node message format node representation type
   * @tparam Effect effect type
   * @return JSON-RPC request client
   */
  inline def basic[Node, CodecType <: Codec[Node], Effect[_]](
    codec: CodecType,
    backend: Backend[Effect],
    transport: Transport[Effect, NoContext]
  ): JsonRpcClient[Node, CodecType, Effect, NoContext] =
    new JsonRpcClient(codec, backend, transport)
