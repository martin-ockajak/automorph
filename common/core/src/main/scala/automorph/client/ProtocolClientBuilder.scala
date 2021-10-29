package automorph.client

import automorph.Client
import automorph.spi.transport.ClientMessageTransport
import automorph.spi.{MessageCodec, RpcProtocol}

/**
 * RPC client builder.
 *
 * @constructor Creates a new RPC client builder.
 * @param protocol RPC protocol plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
case class ProtocolClientBuilder[Node, Codec <: MessageCodec[Node]](
  protocol: RpcProtocol[Node, Codec]
) {

  /**
   * Set specified effect transport plugin.
   *
   * @param transport effect transport plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request client builder
   */
  def transport[Effect[_], Context](
    transport: ClientMessageTransport[Effect, Context]
  ): Client[Node, Codec, Effect, Context] = Client(protocol, transport)
}
