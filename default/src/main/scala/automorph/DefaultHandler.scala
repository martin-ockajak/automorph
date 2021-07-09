package automorph

import automorph.DefaultTypes.DefaultHandler
import automorph.Handler
import automorph.backend.IdentityBackend.Identity
import automorph.spi.Backend
import automorph.util.EmptyContext
import scala.concurrent.{ExecutionContext, Future}

case object DefaultHandler {

  /**
   * Creates a default request handler with specified request `Context` type and specified ''backend'' plugin and.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @param backend effectful computation backend plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return request handler
   */
  def apply[Effect[_], Context](backend: Backend[Effect]): DefaultHandler[Effect, Context] =
    Handler(DefaultCodec(), backend)

  /**
   * Creates a default asynchronous request handler with specified request `Context` type and 'Future' as an effect type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @param executionContext execution context
   * @return asynchronous request handler
   */
  def async[Context]()(implicit executionContext: ExecutionContext): DefaultHandler[Future, Context] =
    Handler(DefaultCodec(), DefaultBackend.async)

  /**
   * Creates a default synchronous request handler with specified request `Context` type and identity as an effect type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @return synchronous request handler
   */
  def sync[Context](): DefaultHandler[Identity, Context] =
    Handler(DefaultCodec(), DefaultBackend.sync)

  /**
   * Creates a default request handler with empty request context and specified ''backend'' plugin.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @param backend effect backend plugin
   * @tparam Effect effect type
   * @return request handler
   */
  def withoutContext[Effect[_]](backend: Backend[Effect]): DefaultHandler[Effect, EmptyContext.Value] =
    Handler.withoutContext(DefaultCodec(), backend)

  /**
   * Creates a default asynchronous request handler with empty request context and 'Future' as an effect type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @param executionContext execution context
   * @return asynchronous request handler
   */
  def asyncWithoutContext()(implicit executionContext: ExecutionContext): DefaultHandler[Future, EmptyContext.Value] =
    Handler.withoutContext(DefaultCodec(), DefaultBackend.async)

  /**
   * Creates a default synchronous request handler with empty request context and identity as an effect type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @see [[https://www.jsonrpc.org/specification protocol specification]]
   * @param executionContext execution context
   * @return asynchronous request handler
   */
  def syncWithoutContext(): DefaultHandler[Identity, EmptyContext.Value] =
    Handler.withoutContext(DefaultCodec(), DefaultBackend.sync)
}
