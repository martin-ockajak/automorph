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
 * @constructor Creates a RPC client with specified ''system'', ''protocol'' and ''transport'' plugins accepting corresponding request context type.
 * @param system effect system plugin
 * @param protocol RPC protocol
 * @param transport message transport plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  system: EffectSystem[Effect],
  protocol: RpcProtocol[Node, Codec],
  transport: ClientMessageTransport[Effect, Context]
) extends ClientCore[Node, Codec, Effect, Context]
  with ClientMeta[Node, Codec, Effect, Context]
  with CannotEqual
  with Logging

object Client {

  /**
   * Creates a RPC client with specified ''system'', ''protocol'' and ''transport'' plugins without accepting request context.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param system effect system plugin
   * @param protocol RPC protocol
   * @param transport message transport protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @return RPC client
   */
  def withoutContext[Node, Codec <: MessageCodec[Node], Effect[_]](
    system: EffectSystem[Effect],
    protocol: RpcProtocol[Node, Codec],
    transport: ClientMessageTransport[Effect, EmptyContext.Value]
  ): Client[Node, Codec, Effect, EmptyContext.Value] = Client(system, protocol, transport)
}
