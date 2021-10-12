package automorph.client

import automorph.Client
import automorph.spi.transport.ClientMessageTransport
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.EmptyContext

/**
 * RPC request handler builder.
 *
 * @constructor Creates a new RPC request handler builder.
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
case class ClientBuilder[Node, Codec <: MessageCodec[Node], Effect[_], Context] (
  private val protocol: Option[RpcProtocol[Node, Codec]] = None,
  private val transport: Option[ClientMessageTransport[Effect, Context]] = None,
) {

  /**
   * Set specified RPC ''protocol'' plugin.
   *
   * @param protocol RPC ''protocol'' plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return RPC request handler builder
   */
  def protocol[Node, Codec <: MessageCodec[Node]](protocol: RpcProtocol[Node, Codec]): ClientBuilder[Node, Codec, Effect, Context] =
    copy(protocol = Some(protocol))

  /**
   * Set specified RPC ''transport'' plugin.
   *
   * @param transport RPC ''transport'' plugin
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler builder
   */
  def transport[Effect[_], Context](transport: ClientMessageTransport[Effect, Context]): ClientBuilder[Node, Codec, Effect, Context] =
    copy(transport = Some(transport))

  /**
   * Build an RPC request handler.
   *
   * @return RPC request handler
   */
  def build: Client[Node, Codec, Effect, Context] =
    Client(get(protocol, "RPC protocol"), get(transport, "message transport"))

  private def get[T](plugin: Option[T], name: String): T =
    plugin.getOrElse(throw new IllegalStateException(s"Missing $name plugin"))
}
