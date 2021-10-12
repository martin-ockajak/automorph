package automorph

import automorph.client.{ClientCore, ClientMeta, ProtocolClientBuilder, TransportClientBuilder}
import automorph.log.Logging
import automorph.spi.transport.ClientMessageTransport
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.{CannotEqual, Context}

/**
 * RPC client.
 *
 * Used to perform remote API calls and notifications.
 *
 * @constructor Creates a RPC client with specified protocol and transport plugins accepting corresponding request context type.
 * @param protocol RPC protocol plugin
 * @param transport message transport plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  protocol: RpcProtocol[Node, Codec],
  transport: ClientMessageTransport[Effect, Context]
) extends ClientCore[Node, Codec, Effect, Context]
  with ClientMeta[Node, Codec, Effect, Context]
  with CannotEqual
  with Logging {
  protected val system = transport.system
}

object Client {

  /**
   * Creates an RPC client builder with specified RPC protocol plugin.
   *
   * @param protocol RPC protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return RPC client builder
   */
  def protocol[Node, Codec <: MessageCodec[Node]](
    protocol: RpcProtocol[Node, Codec]
  ): ProtocolClientBuilder[Node, Codec] =
    ProtocolClientBuilder(protocol)

  /**
   * Creates an RPC client builder with specified effect transport plugin.
   *
   * @param transport message transport plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC client builder
   */
  def transport[Effect[_], Context](
    transport: ClientMessageTransport[Effect, Context]
  ): TransportClientBuilder[Effect, Context] =
    TransportClientBuilder(transport)
}
