package automorph

import automorph.client.{ClientBind, ClientCore, NamedMethodProxy}
import automorph.log.Logging
import automorph.protocol.JsonRpcProtocol
import automorph.spi.transport.ClientMessageTransport
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}

/**
 * Automorph RPC client.
 *
 * Used to perform RPC calls and notifications.
 *
 * @constructor Creates a RPC client with specified request `Context` type plus ''codec'', ''system'' and ''transport'' plugins.
 * @param codec message codec plugin
 * @param system effect system plugin
 * @param transport message transport plugin
 * @param protocol RPC protocol
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  codec: Codec,
  system: EffectSystem[Effect],
  transport: ClientMessageTransport[Effect, Context],
  protocol: RpcProtocol[Node]
) extends ClientCore[Node, Codec, Effect, Context]
  with ClientBind[Node, Codec, Effect, Context]
  with CannotEqual
  with Logging

case object Client {

  /**
   * Creates a RPC client with specified request `Context` type plus ''codec'', ''system'' and ''transport'' plugins.
   *
   * The client can be used to perform RPC calls and notifications.
   * @param codec message codec plugin
   * @param system effect system plugin
   * @param transport message transport plugin
   * @param protocol RPC protocol
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   */
  def apply[Node, Codec <: MessageCodec[Node], Effect[_], Context](
    codec: Codec,
    system: EffectSystem[Effect],
    transport: ClientMessageTransport[Effect, Context]
  ): Client[Node, Codec, Effect, Context] = Client(codec, system, transport, JsonRpcProtocol(codec))

  /**
   * Creates a RPC client with empty request context and specified ''codec'', ''system'' and ''transport'' plugins.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param codec structured message codec codec plugin
   * @param system effect system plugin
   * @param transport message transport protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @return RPC client
   */
  def withoutContext[Node, Codec <: MessageCodec[Node], Effect[_]](
    codec: Codec,
    system: EffectSystem[Effect],
    transport: ClientMessageTransport[Effect, EmptyContext.Value]
  ): Client[Node, Codec, Effect, EmptyContext.Value] = Client(codec, system, transport, JsonRpcProtocol(codec))
}
