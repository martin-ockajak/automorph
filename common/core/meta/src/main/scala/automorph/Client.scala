package automorph

import automorph.client.{ClientCore, ClientMeta}
import automorph.log.Logging
import automorph.spi.transport.ClientMessageTransport
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}

/**
 * Automorph RPC client.
 *
 * Used to perform RPC calls and notifications.
 *
 * @constructor Creates a RPC client with specified request `Context` type plus ''codec'', ''system'', ''transport'' and ''protocol'' plugins.
 * @param codec message codec plugin
 * @param system effect system plugin
 * @param protocol RPC protocol
 * @param transport message transport plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  codec: Codec,
  system: EffectSystem[Effect],
  protocol: RpcProtocol[Node],
  transport: ClientMessageTransport[Effect, Context]
) extends ClientCore[Node, Codec, Effect, Context]
  with ClientMeta[Node, Codec, Effect, Context]
  with CannotEqual
  with Logging

object Client {

  /**
   * Creates a RPC client with empty request context and specified ''codec'', ''system'' and ''transport'' plugins.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param codec structured message codec codec plugin
   * @param system effect system plugin
   * @param protocol RPC protocol
   * @param transport message transport protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @return RPC client
   */
  def withoutContext[Node, Codec <: MessageCodec[Node], Effect[_]](
    codec: Codec,
    system: EffectSystem[Effect],
    protocol: RpcProtocol[Node],
    transport: ClientMessageTransport[Effect, EmptyContext.Value]
  ): Client[Node, Codec, Effect, EmptyContext.Value] = Client(codec, system, protocol, transport)
}
