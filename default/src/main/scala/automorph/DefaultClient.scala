package automorph

import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem.Identity
import scala.concurrent.{ExecutionContext, Future}

case object DefaultClient {

  /**
   * Default client type.
   *
   * @tparam Effect effect type
   * @tparam Context request context type
   */
  type Type[Effect[_], Context] = Client[DefaultMessageFormat.Node, DefaultMessageFormat.Type, Effect, Context]

  /**
   * Creates a default RPC client with specified effect ''system'' and message ''transport'' plugins.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param system effect system plugin
   * @param transport message transport protocol plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC client
   */
  def apply[Effect[_], Context](
    system: EffectSystem[Effect],
    transport: ClientMessageTransport[Effect, Context]
  ): Client[DefaultMessageFormat.Node, DefaultMessageFormat.Type, Effect, Context] =
    Client(DefaultMessageFormat(), system, transport)

  /**
   * Creates a default asynchronous RPC client using 'Future' as an effect type with specified message ''transport'' plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param transport asynchronous message transport protocol plugin
   * @param executionContext execution context
   * @tparam Context request context type
   * @return asynchronous RPC client
   */
  def async[Context](transport: ClientMessageTransport[Future, Context])(implicit
    executionContext: ExecutionContext
  ): Type[Future, Context] = DefaultClient(DefaultEffectSystem.async, transport)

  /**
   * Creates a default asynchronous RPC client using identity as an effect type with specified message ''transport'' plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param transport synchronous message transport protocol plugin
   * @tparam Context request context type
   * @return synchronous RPC client
   */
  def sync[Context](
    transport: ClientMessageTransport[Identity, Context]
  ): Type[Identity, Context] = DefaultClient(DefaultEffectSystem.sync, transport)

}
