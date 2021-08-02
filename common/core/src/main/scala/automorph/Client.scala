package automorph

import automorph.Client.defaultErrorMapping
import automorph.client.{ClientBind, ClientCore, NamedMethodProxy}
import automorph.protocol.jsonrpc.ErrorType
import automorph.protocol.jsonrpc.ErrorType.{InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}
import automorph.spi.{EffectSystem, MessageFormat, ClientMessageTransport}
import automorph.util.{CannotEqual, EmptyContext}
import java.io.IOException

/**
 * Automorph RPC client.
 *
 * Used to perform RPC calls and notifications.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a RPC client with specified request `Context` type plus ''format'', ''system'' and ''transport'' plugins.
 * @param format message format plugin
 * @param system effect system plugin
 * @param transport message transport plugin
 * @param errorToException maps a JSON-RPC error to a corresponding exception
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, Format <: MessageFormat[Node], Effect[_], Context](
  format: Format,
  system: EffectSystem[Effect],
  transport: ClientMessageTransport[Effect, Context],
  protected val errorToException: (Int, String) => Throwable = defaultErrorMapping
) extends ClientBind[Node, Format, Effect, Context] with AutoCloseable with CannotEqual {

  /** This client type. */
  type ThisClient = Client[Node, Format, Effect, Context]
  /** Named method proxy type. */
  type NamedMethod = NamedMethodProxy[Node, Format, Effect, Context]

  val core: ClientCore[Node, Format, Effect, Context] = ClientCore(format, system, transport, errorToException)

  /**
   * Creates a method proxy with specified method name.
   *
   * @param methodName method name
   * @return method proxy with specified method name
   */
  def method(methodName: String): NamedMethod = NamedMethodProxy(methodName, core, Seq(), Seq())

  /**
   * Creates default request context.
   *
   * @return request context
   */
  def context: Context = transport.defaultContext

  /**
   * Creates a copy of this client with specified JSON-RPC error to exception mapping.
   *
   * @param errorToException maps a JSON-RPC error to a corresponding exception
   * @return JSON-RPC server with the specified JSON-RPC error to exception mapping
   */
  def errorMapping(errorToException: (Int, String) => Throwable): ThisClient =
    copy(errorToException = errorToException)

  override def close(): Unit = transport.close()

  override def toString: String =
    s"${this.getClass.getName}(format = ${format.getClass.getName}, system = ${system.getClass.getName}, transport = ${transport.getClass.getName})"
}

case object Client {

  /**
   * Creates a RPC client with empty request context and specified ''format'', ''system'' and ''transport'' plugins.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param format structured message format format plugin
   * @param system effect system plugin
   * @param transport message transport protocol plugin
   * @tparam Node message node type
   * @tparam Format message format plugin type
   * @tparam Effect effect type
   * @return RPC client
   */
  def withoutContext[Node, Format <: MessageFormat[Node], Effect[_]](
    format: Format,
    system: EffectSystem[Effect],
    transport: ClientMessageTransport[Effect, EmptyContext.Value]
  ): Client[Node, Format, Effect, EmptyContext.Value] = Client(format, system, transport)

  /**
   * Maps a JSON-RPC error to a corresponding default exception.
   *
   * @param code error code
   * @param message error message
   * @return exception
   */
  def defaultErrorMapping(code: Int, message: String): Throwable = code match {
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
