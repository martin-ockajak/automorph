package automorph.client

import automorph.Client
import automorph.spi.{ClientMessageTransport, MessageCodec, RpcProtocol}

/**
 * RPC request client builder.
 *
 * @constructor
 *   Creates a new RPC client builder.
 * @param transport
 *   message transport plugin
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   request context type
 */
case class TransportClientBuilder[Effect[_], Context](transport: ClientMessageTransport[Effect, Context]) {

  /**
   * Creates a new RPC client with specified RPC protocol plugin.
   *
   * @param protocol
   *   RPC protocol plugin
   * @tparam Node
   *   message node type
   * @tparam Codec
   *   message codec plugin type
   * @return
   *   RPC request client builder
   */
  def protocol[Node, Codec <: MessageCodec[Node]](
    protocol: RpcProtocol[Node, Codec, Context]
  ): Client[Node, Codec, Effect, Context] =
    Client(protocol, transport)
}
