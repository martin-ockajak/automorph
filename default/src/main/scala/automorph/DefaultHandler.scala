package automorph

import automorph.DefaultTypes.DefaultHandler
import automorph.Handler
import automorph.backend.IdentityBackend.Identity
import automorph.backend.{FutureBackend, IdentityBackend}
import automorph.codec.common.UpickleCustom
import automorph.codec.json.UpickleJsonCodec
import automorph.spi.Backend
import automorph.util.EmptyContext
import scala.concurrent.{ExecutionContext, Future}

case object DefaultHandler {

  /**
   * Creates a JSON-RPC request handler with specified request `Context` type and specified ''backend'' plugin and.
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
   * Creates an asynchronous JSON-RPC request handler with specified request `Context` type and 'Future' as an effect type.
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
   * Creates a synchronous JSON-RPC request handler with specified request `Context` type and identity as an effect type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @return synchronous JSON-RPC request handler
   */
  def sync[Context](): DefaultHandler[Identity, Context] =
    Handler(UpickleJsonCodec(), IdentityBackend())

  /**
   * Creates a JSON-RPC request handler with empty request context and specified ''backend'' plugin.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param backend effect backend plugin
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def withoutContext[Effect[_]](backend: Backend[Effect]): DefaultHandler[Effect, EmptyContext.Value] =
    Handler.withoutContext(UpickleJsonCodec(), backend)

  /**
   * Creates an asynchronous JSON-RPC request handler with empty request context and 'Future' as an effect type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def asyncWithoutContext()(implicit executionContext: ExecutionContext): DefaultHandler[Future, EmptyContext.Value] =
    Handler.withoutContext(UpickleJsonCodec(), FutureBackend())

  /**
   * Creates a synchronous JSON-RPC request handler with empty request context and identity as an effect type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param executionContext execution context
   * @return asynchronous JSON-RPC request handler
   */
  def syncWithoutContext(): DefaultHandler[Identity, EmptyContext.Value] =
    Handler.withoutContext(UpickleJsonCodec(), IdentityBackend())
}
