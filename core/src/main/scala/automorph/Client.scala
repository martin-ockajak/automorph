package automorph

import automorph.Client.defaultErrorToException
import automorph.client.{ClientCore, ClientBind, PositionalMethodProxy}
import automorph.protocol.ErrorType
import automorph.protocol.ErrorType.{InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}
import automorph.spi.{Backend, Codec, Transport}
import automorph.util.{CannotEqual, NoContext}
import java.io.IOException

/**
 * JSON-RPC client.
 *
 * The client can be used to perform JSON-RPC calls and notifications.
 *
 * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
 * @constructor Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins with defined request `Context` type.
 * @param codec message codec plugin
 * @param backend effect backend plugin
 * @param transport message transport plugin
 * @param errorToException mapping of JSON-RPC errors to exceptions
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  codec: ExactCodec,
  backend: Backend[Effect],
  transport: Transport[Effect, Context],
  namedArguments: Boolean = true,
  protected val errorToException: (Int, String) => Throwable = defaultErrorToException
) extends ClientCore[Node, ExactCodec, Effect, Context]
  with ClientBind[Node, ExactCodec, Effect, Context]
  with CannotEqual {

  type ThisClient = Client[Node, ExactCodec, Effect, Context]
  type PositionalMethod = PositionalMethodProxy[Node, ExactCodec, Effect, Context]

  /**
   * Create a method invoker with specified method name.
   *
   * @param methodName method name
   * @return method invoker with specified method name
   */
  def method(methodName: String): PositionalMethod =
    PositionalMethodProxy(methodName, codec, backend, transport, errorToException, Seq(), Seq())

  /**
   * Create a copy of this client passing method arguments ''by name''.
   *
   * @return client method invoker passing method arguments ''by name''
   */
  def named: ThisClient = copy(namedArguments = false)

  /**
   * Create a copy of this client passing method arguments ''by position''.
   *
   * @return client method invoker passing method arguments ''by position''
   */
  def positional: ThisClient = copy(namedArguments = true)

  /**
   * Create a copy of this client with specified JSON-RPC error to exception mapping.
   *
   * @param errorToException JSON-RPC error to exception mapping
   * @return JSON-RPC server with the specified JSON-RPC error to exception mapping
   */
  def mapErrors(errorToException: (Int, String) => Throwable): ThisClient =
    copy(errorToException = errorToException)

  override def toString: String =
    s"${this.getClass.getName}(codec = ${codec.getClass.getName}, backend = ${backend.getClass.getName}, transport = ${transport.getClass.getName})"
}

case object Client {

  /**
   * Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins with empty request `Context` type.
   *
   * The client can be used to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @param transport message transport plugin
   * @tparam Node message format node representation type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC client
   */
  def noContext[Node, ExactCodec <: Codec[Node], Effect[_]](
    codec: ExactCodec,
    backend: Backend[Effect],
    transport: Transport[Effect, NoContext.Value]
  ): Client[Node, ExactCodec, Effect, NoContext.Value] = Client(codec, backend, transport)

  /**
   * Default JSON-RPC error to exception mapping.
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
