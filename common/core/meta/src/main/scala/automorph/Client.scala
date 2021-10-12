package automorph

import automorph.client.{ClientBuilder, ClientCore, ClientMeta}
import automorph.log.Logging
import automorph.spi.transport.ClientMessageTransport
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}

/**
 * RPC client.
 *
 * Used to perform remote API calls and notifications.
 *
 * @constructor Creates a RPC client with specified ''protocol'' and ''transport'' plugins accepting corresponding request context type.
 * @param protocol RPC ''protocol'' plugin
 * @param transport message ''transport'' plugin
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
   * Creates a RPC client with specified ''protocol'' and ''transport'' plugins without accepting request context.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param protocol RPC protocol
   * @param transport message transport protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @return RPC client
   */
  def withoutContext[Node, Codec <: MessageCodec[Node], Effect[_]](
    protocol: RpcProtocol[Node, Codec],
    transport: ClientMessageTransport[Effect, EmptyContext.Value]
  ): Client[Node, Codec, Effect, EmptyContext.Value] = Client(protocol, transport)

  /**
   * Creates an RPC client builder.
   *
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @return RPC request handler builder
   */
  def builder[Node, Codec <: MessageCodec[Node], Effect[_]]: ClientBuilder[Node, Codec, Effect, EmptyContext.Value] =
    ClientBuilder()
}
