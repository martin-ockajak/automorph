package automorph

import automorph.Client.defaultErrorToException
import automorph.client.{ClientBind, ClientCore, NamedMethodProxy}
import automorph.protocol.ErrorType
import automorph.protocol.ErrorType.{InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}
import automorph.spi.{Backend, Codec, Transport}
import automorph.util.{CannotEqual, EmptyContext}
import java.io.IOException

/**
 * Automorph RPC client.
 *
 * Used to perform RPC calls and notifications.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a JSON-RPC client with specified request `Context` type plus ''codec'', ''backend'' and ''transport'' plugins.
 * @param codec structured message format codec plugin
 * @param backend effectful computation backend plugin
 * @param transport message transport protocol plugin
 * @param errorToException maps a JSON-RPC error to a corresponding exception
 * @tparam Node message node type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  codec: ExactCodec,
  backend: Backend[Effect],
  transport: Transport[Effect, Context],
  protected val errorToException: (Int, String) => Throwable = defaultErrorToException
) extends ClientBind[Node, ExactCodec, Effect, Context] with CannotEqual {

  type ThisClient = Client[Node, ExactCodec, Effect, Context]
  type NamedMethod = NamedMethodProxy[Node, ExactCodec, Effect, Context]

  val core = ClientCore(codec, backend, transport, errorToException)

  /**
   * Creates a method proxy with specified method name.
   *
   * @param methodName method name
   * @return method proxy with specified method name
   */
  def method(methodName: String): NamedMethod = NamedMethodProxy(methodName, core, Seq(), Seq())

  /**
   * Create default request context.
   *
   * @return request context
   */
  def defaultContext: Context = transport.defaultContext

  /**
   * Creates a copy of this client with specified JSON-RPC error to exception mapping.
   *
   * @param errorToException maps a JSON-RPC error to a corresponding exception
   * @return JSON-RPC server with the specified JSON-RPC error to exception mapping
   */
  def errorMapping(errorToException: (Int, String) => Throwable): ThisClient =
    copy(errorToException = errorToException)

  override def toString: String =
    s"${this.getClass.getName}(codec = ${codec.getClass.getName}, backend = ${backend.getClass.getName}, transport = ${transport.getClass.getName})"
}

case object Client {

  /**
   * Creates a JSON-RPC client with empty request context and specified ''codec'', ''backend'' and ''transport'' plugins.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec structured message format codec plugin
   * @param backend effectful computation backend plugin
   * @param transport message transport protocol plugin
   * @tparam Node message node type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC client
   */
  def withoutContext[Node, ExactCodec <: Codec[Node], Effect[_]](
    codec: ExactCodec,
    backend: Backend[Effect],
    transport: Transport[Effect, EmptyContext.Value]
  ): Client[Node, ExactCodec, Effect, EmptyContext.Value] = Client(codec, backend, transport)

  /**
   * Maps a JSON-RPC error to a corresponding default exception.
   *
   * @param code error code
   * @param message error message
   * @return exception
   */
  def defaultErrorToException(code: Int, message: String): Throwable = code match {
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
