package automorph

import automorph.spi.EffectSystem
import automorph.system.IdentitySystem.Identity
import scala.concurrent.{ExecutionContext, Future}

object DefaultHandler {

  /**
   * Creates a default asynchronous RPC request handler with specified effect system plugin and providing given request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param system effect system plugin
   * @tparam Context request context type
   * @return RPC request handler
   */
  def apply[Effect[_], Context](system: EffectSystem[Effect]): Type[Effect, Context] =
    Handler(DefaultRpcProtocol(), system)

  /**
   * Default request handler type.
   *
   * @tparam Effect effect type
   * @tparam Context request context type
   */
  type Type[Effect[_], Context] = Handler[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Effect, Context]

  /**
   * Creates a default asynchronous RPC request handler using 'Future' as an effect type and providing given request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param executionContext execution context
   * @tparam Context request context type
   * @return asynchronous RPC request handler
   */
  def async[Context](implicit executionContext: ExecutionContext): Type[Future, Context] =
    Handler(DefaultRpcProtocol(), DefaultEffectSystem.async)

  /**
   * Creates a default synchronous RPC request handler using identity as an effect type and providing given request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @tparam Context request context type
   * @return synchronous RPC request handler
   */
  def sync[Context]: Type[Identity, Context] =
    Handler(DefaultRpcProtocol(), DefaultEffectSystem.sync)
}
