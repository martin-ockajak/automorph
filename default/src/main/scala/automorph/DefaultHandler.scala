package automorph

import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem
import automorph.util.EmptyContext
import scala.concurrent.{ExecutionContext, Future}

case object DefaultHandler {

  /** Default requet handler type. */
  type Type[Effect[_], Context] = Handler[DefaultMessageFormat.Node, DefaultMessageFormat.Type, Effect, Context]

  /**
   * Creates a default RPC request handler with specified request `Context` type and specified effect ''system'' plugin.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param system effect system plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  def apply[Effect[_], Context](system: EffectSystem[Effect]): Type[Effect, Context] =
    Handler(DefaultMessageFormat(), system)

  /**
   * Creates a default asynchronous RPC request handler with specified request `Context` type and 'Future' as an effect type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param executionContext execution context
   * @return asynchronous RPC request handler
   */
  def async[Context]()(implicit executionContext: ExecutionContext): Type[Future, Context] =
    Handler(DefaultMessageFormat(), DefaultEffectSystem.async)

  /**
   * Creates a default synchronous RPC request handler with specified request `Context` type and identity as an effect type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @return synchronous RPC request handler
   */
  def sync[Context](): Type[Identity, Context] = Handler(DefaultMessageFormat(), DefaultEffectSystem.sync)

  /**
   * Creates a default request RPC handler with empty request context and specified effect ''system'' plugin.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return RPC request handler
   */
  def withoutContext[Effect[_]](system: EffectSystem[Effect]): Type[Effect, EmptyContext.Value] =
    Handler.withoutContext(DefaultMessageFormat(), system)

  /**
   * Creates a default asynchronous RPC request handler with empty request context and `Future` as an effect type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param executionContext execution context
   * @return asynchronous RPC request handler
   */
  def asyncWithoutContext()(implicit executionContext: ExecutionContext): Type[Future, EmptyContext.Value] =
    Handler.withoutContext(DefaultMessageFormat(), DefaultEffectSystem.async)

  /**
   * Creates a default synchronous RPC request handler with empty request context and `Identity` as an effect type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @return asynchronous RPC request handler
   */
  def syncWithoutContext(): Type[Identity, EmptyContext.Value] =
    Handler.withoutContext(DefaultMessageFormat(), DefaultEffectSystem.sync)
}
