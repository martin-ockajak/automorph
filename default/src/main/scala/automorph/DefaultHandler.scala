package automorph

import automorph.handler.FullHandlerBuilder
import automorph.spi.EffectSystem
import automorph.system.IdentitySystem.Identity
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
   * Creates a default asynchronous RPC request ''handler'' using 'Future' as an effect type and providing given request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @param executionContext execution context
   * @tparam Context request context type
   * @return asynchronous RPC request ''handler''
   */
  def async[Context](implicit executionContext: ExecutionContext): Type[Future, Context] =
    Handler(DefaultRpcProtocol(), DefaultEffectSystem.async)

  /**
   * Creates a default synchronous RPC request ''handler'' using identity as an effect type and providing given request context type.
   *
   * The handler can be used by a server to invoke bound API methods based on incoming requests.
   *
   * @tparam Context request context type
   * @return synchronous RPC request ''handler''
   */
  def sync[Context]: Type[Identity, Context] =
    Handler(DefaultRpcProtocol(), DefaultEffectSystem.sync)
}
