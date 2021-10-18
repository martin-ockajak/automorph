package automorph

import automorph.spi.transport.ClientMessageTransport

object DefaultClient {

  /**
   * Default RPC client type.
   *
   * @tparam Effect effect type
   * @tparam Context request context type
   */
  type Type[Effect[_], Context] = Client[DefaultMessageCodec.Node, DefaultMessageCodec.Type, Effect, Context]

  /**
   * Creates a default RPC client with specified message transport plugin.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param transport message transport protocol plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC client
   */
  def apply[Effect[_], Context](transport: ClientMessageTransport[Effect, Context]): Type[Effect, Context] =
    Client(DefaultRpcProtocol(), transport)
}
