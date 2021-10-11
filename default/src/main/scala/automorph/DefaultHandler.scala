package automorph

import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem
import automorph.util.EmptyContext
import scala.concurrent.{ExecutionContext, Future}

object DefaultHandler {

  /**
   * Default request handler type.
   *
   * @tparam Effect effect type
   * @tparam Context request context type
   */
  type Type[Effect[_], Context] = Handler[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Effect, Context]

  /**
   * Creates a default RPC request handler with specified effect ''system'' plugin providing corresponding request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param system effect system plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  def apply[Effect[_], Context](system: EffectSystem[Effect]): Type[Effect, Context] =
    Handler(system, DefaultRpcProtocol())

  /**
   * Creates a default asynchronous RPC request handler using 'Future' as an effect type and providing corresponding request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param executionContext execution context
   * @tparam Context request context type
   * @return asynchronous RPC request handler
   */
  def async[Context](implicit executionContext: ExecutionContext): Type[Future, Context] =
    Handler(DefaultEffectSystem.async, DefaultRpcProtocol())

  /**
   * Creates a default synchronous RPC request handler using identity as an effect type and providing corresponding request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @tparam Context request context type
   * @return synchronous RPC request handler
   */
  def sync[Context]: Type[Identity, Context] =
    Handler(DefaultEffectSystem.sync, DefaultRpcProtocol())

  /**
   * Creates a default request RPC handler with specified effect ''system'' plugin without providing request context.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return RPC request handler
   */
  def withoutContext[Effect[_]](system: EffectSystem[Effect]): Type[Effect, EmptyContext.Value] =
    Handler.withoutContext(system, DefaultRpcProtocol())

  /**
   * Creates a default asynchronous RPC handler using `Future` as an effect type without providing request context.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param executionContext execution context
   * @return asynchronous RPC request handler
   */
  def asyncWithoutContext()(implicit executionContext: ExecutionContext): Type[Future, EmptyContext.Value] =
    Handler.withoutContext(DefaultEffectSystem.async, DefaultRpcProtocol())

  /**
   * Creates a default synchronous RPC request handler using `Identity` as an effect type without providing request context.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @return asynchronous RPC request handler
   */
  def syncWithoutContext(): Type[Identity, EmptyContext.Value] =
    Handler.withoutContext(DefaultEffectSystem.sync, DefaultRpcProtocol())
}
