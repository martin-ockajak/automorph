package automorph

import automorph.DefaultTypes.DefaultHandler
import automorph.Handler
import automorph.backend.IdentityBackend.Identity
import automorph.backend.{FutureBackend, IdentityBackend}
import automorph.codec.common.UpickleCustom
import automorph.codec.json.UpickleJsonCodec
import automorph.spi.Backend
import automorph.util.NoContext
import scala.concurrent.{ExecutionContext, Future}
import ujson.Value

case object DefaultHandler {

  /**
   * Creates a JSON-RPC request handler using the specified ''backend'' plugin with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param backend effectful computation backend plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return JSON-RPC request handler
   */
  def apply[Effect[_], Context](backend: Backend[Effect]): DefaultHandler[Effect, Context] =
    Handler(UpickleJsonCodec(), backend)

  /**
   * Creates an asynchronous JSON-RPC request handler with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def async[Context]()(implicit executionContext: ExecutionContext): DefaultHandler[Future, Context] =
    Handler(UpickleJsonCodec(), FutureBackend())

  /**
   * Creates a synchronous JSON-RPC request handler with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @return synchronous JSON-RPC request handler
   */
  def sync[Context](): DefaultHandler[Identity, Context] =
    Handler(UpickleJsonCodec(), IdentityBackend())

  /**
   * Creates a JSON-RPC request handler using the specified ''backend'' plugin with empty request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param backend effect backend plugin
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def noContext[Effect[_]](backend: Backend[Effect]): DefaultHandler[Effect, NoContext.Value] =
    Handler.noContext(UpickleJsonCodec(), backend)

  /**
   * Creates an asynchronous JSON-RPC request handler with empty request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def asyncNoContext()(implicit executionContext: ExecutionContext): DefaultHandler[Future, NoContext.Value] =
    Handler.noContext(UpickleJsonCodec(), FutureBackend())

  /**
   * Creates a synchronous JSON-RPC request handler with empty request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def syncNoContext(): DefaultHandler[Identity, NoContext.Value] =
    Handler.noContext(UpickleJsonCodec(), IdentityBackend())
}
