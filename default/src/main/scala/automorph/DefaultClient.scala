package automorph

import automorph.client.ProtocolClientBuilder
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.IdentitySystem.Identity
import automorph.util.EmptyContext
import scala.concurrent.{ExecutionContext, Future}

object DefaultClient {

  /**
   * Default client type.
   *
   * @tparam Effect effect type
   * @tparam Context request context type
   */
  type Type[Effect[_], Context] = Client[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Effect, Context]

  /**
   * Creates a default RPC ''client'' with specified ''system'' and ''transport'' plugins accepting given request context type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param transport message transport protocol plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC ''client''
   */
  def apply[Effect[_], Context](transport: ClientMessageTransport[Effect, Context]): Type[Effect, Context] =
    Client(DefaultRpcProtocol(), transport)

  /**
   * Creates a default asynchronous RPC ''client'' with specified message transport'' plugin using 'Future' as an effect type and accepting given request context type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param transport asynchronous message transport protocol plugin
   * @param executionContext execution context
   * @tparam Context request context type
   * @return asynchronous RPC ''client''
   */
  def async[Context](transport: ClientMessageTransport[Future, Context])(implicit
    executionContext: ExecutionContext
  ): Type[Future, Context] = DefaultClient(transport)

  /**
   * Creates a default asynchronous RPC ''client'' with specified message transport'' plugin using identity as an effect type and accepting given request context type.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param transport synchronous message transport protocol plugin
   * @tparam Context request context type
   * @return synchronous RPC ''client''
   */
  def sync[Context](transport: ClientMessageTransport[Identity, Context]): Type[Identity, Context] =
    DefaultClient(transport)


  /**
   * Creates a default synchronous RPC ''client'' builder using identity as an effect type and accepting given request context type.
   *
   * @return RPC request ''client'' builder
   */
  def builder: ProtocolClientBuilder[DefaultMessageCodec.Node, DefaultMessageCodec.Type] =
    Client.protocol(DefaultRpcProtocol())
}
