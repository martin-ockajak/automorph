package automorph

import automorph.client.{ClientBind, ClientCore, NamedMethodProxy}
import automorph.protocol.jsonrpc.JsonRpcProtocol
import automorph.spi.{ClientMessageTransport, EffectSystem, MessageFormat, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}

/**
 * Automorph RPC client.
 *
 * Used to perform RPC calls and notifications.
 *
 * @constructor Creates a RPC client with specified request `Context` type plus ''format'', ''system'' and ''transport'' plugins.
 * @param format message format plugin
 * @param system effect system plugin
 * @param transport message transport plugin
 * @param protocol RPC protocol
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Client[Node, Format <: MessageFormat[Node], Effect[_], Context](
  format: Format,
  system: EffectSystem[Effect],
  transport: ClientMessageTransport[Effect, Context],
  protocol: RpcProtocol = JsonRpcProtocol()
) extends ClientBind[Node, Format, Effect, Context] with AutoCloseable with CannotEqual {

  /** This client type. */
  type ThisClient = Client[Node, Format, Effect, Context]
  /** Named method proxy type. */
  type NamedMethod = NamedMethodProxy[Node, Format, Effect, Context]

  val core: ClientCore[Node, Format, Effect, Context] = ClientCore(format, system, transport, protocol)

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

  override def close(): Unit = transport.close()

  override def toString: String =
    s"${this.getClass.getName}(format = ${format.getClass.getName}, system = ${system.getClass.getName}, transport = ${transport.getClass.getName})"
}

case object Client {

  /**
   * Creates a RPC client with empty request context and specified ''format'', ''system'' and ''transport'' plugins.
   *
   * The client can be used to perform RPC calls and notifications.
   *
   * @param format structured message format format plugin
   * @param system effect system plugin
   * @param transport message transport protocol plugin
   * @tparam Node message node type
   * @tparam Format message format plugin type
   * @tparam Effect effect type
   * @return RPC client
   */
  def withoutContext[Node, Format <: MessageFormat[Node], Effect[_]](
    format: Format,
    system: EffectSystem[Effect],
    transport: ClientMessageTransport[Effect, EmptyContext.Value]
  ): Client[Node, Format, Effect, EmptyContext.Value] = Client(format, system, transport, JsonRpcProtocol())
}
