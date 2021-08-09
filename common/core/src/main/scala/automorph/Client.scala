package automorph

import automorph.client.{ClientBind, ClientCore, NamedMethodProxy}
import automorph.protocol.jsonrpc.JsonRpcProtocol
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
  protocol: RpcProtocol = JsonRpcProtocol()
) extends ClientBind[Node, Codec, Effect, Context] with CannotEqual {

  /** This client type. */
  type ThisClient = Client[Node, Codec, Effect, Context]
  /** Named method proxy type. */
  type NamedMethod = NamedMethodProxy[Node, Codec, Effect, Context]

  val core: ClientCore[Node, Codec, Effect, Context] = ClientCore(codec, system, transport, protocol)

  /**
   * Creates a method proxy with specified method name.
   *
   * @param methodName method name
   * @return method proxy with specified method name
   */
  def method(methodName: String): NamedMethod = NamedMethodProxy(methodName, core, Seq.empty, Seq.empty)

  /**
   * Creates default request context.
   *
   * @return request context
   */
  def context: Context = transport.defaultContext

  /**
   * Creates a copy of this client with specified RPC protocol.
   *
   * @param protocol RPC protocol
   * @return RPC request handler
   */
  def protocol(protocol: RpcProtocol): ThisClient = copy(protocol = protocol)

  def close(): Effect[Unit] = system.pure(())

  override def toString: String =
    s"${this.getClass.getName}(codec = ${codec.getClass.getName}, system = ${system.getClass.getName}, transport = ${transport.getClass.getName})"
}

case object Client {

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
  ): Client[Node, Codec, Effect, EmptyContext.Value] = Client(codec, system, transport, JsonRpcProtocol())
}
