package automorph

import automorph.endpoint.meta.EndpointMeta
import automorph.handler.BindingHandler
import automorph.spi.protocol.RpcFunction
import automorph.spi.{EndpointTransport, MessageCodec, RequestHandler, RpcProtocol}
import scala.collection.immutable.ListMap

/**
 * RPC endpoint.
 *
 * Integratesd with an existing message transport layer to receive remote API requests using
 * specific message transport protocol and invoke bound API methods to process them.
 *
 * Automatically derives remote API bindings for existing API instances.
 *
 * @constructor
 *   Creates a RPC endpoint with specified protocol and transport plugins supporting corresponding message context type.
 * @param transport
 *   transport layer transport plugin
 * @param rpcProtocol
 *   RPC protocol plugin
 * @param handler
 *   RPC request handler
 * @param functions
 *   bound RPC functions
 * @tparam Node
 *   message node type
 * @tparam Codec
 *   message codec plugin type
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   RPC message context type
 * @tparam Adapter
 *   transport layer adapter type
 */
final case class Endpoint[Node, Codec <: MessageCodec[Node], Effect[_], Context, Adapter] (
  transport: EndpointTransport[Effect, Context, Adapter],
  rpcProtocol: RpcProtocol[Node, Codec, Context],
  handler: RequestHandler[Effect, Context],
  functions: Seq[RpcFunction] = Seq.empty,
) extends EndpointMeta[Node, Codec, Effect, Context, Adapter] {

  // Register transport request handler.
  private val registeredAdapter = transport.clone(handler)

  /** Transport layer adapter. */
  def adapter: Adapter =
    transport.adapter

  override def toString: String = {
    val plugins = Map[String, Any](
      "rpcProtocol" -> rpcProtocol,
      "transport" -> registeredAdapter,
    ).map { case (name, plugin) =>
      s"$name = ${plugin.getClass.getName}"
    }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }
}

object Endpoint {

  /**
   * RPC endpoint builder.
   *
   * @constructor
   *   Creates a new RPC endpoint builder.
   * @param transport
   *   message transport plugin
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   request context type
   * @tparam Adapter
   *   transport layer transport type
   */
  case class EndpointBuilder[Effect[_], Context, Adapter](transport: EndpointTransport[Effect, Context, Adapter]) {

    /**
     * Creates a new RPC endpoint with specified RPC protocol plugin.
     *
     * @param rpcProtocol
     *   RPC protocol plugin
     * @tparam Node
     *   message node type
     * @tparam Codec
     *   message codec plugin type
     * @return
     *   RPC endpoint builder
     */
    def rpcProtocol[Node, Codec <: MessageCodec[Node]](
      rpcProtocol: RpcProtocol[Node, Codec, Context]
    ): Endpoint[Node, Codec, Effect, Context, Adapter] =
      Endpoint(transport, rpcProtocol)
  }

  /**
   * Creates a RPC server with specified protocol and transport plugins supporting corresponding message context type.
   *
   * @param transport
   *   endpoint transport later transport
   * @param rpcProtocol
   * RPC protocol plugin
   * @tparam Node
   *   message node type
   * @tparam Codec
   *   message codec plugin type
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   RPC message context type
   * @tparam Adapter
   *   transport layer transport type
   * @return RPC server
   */
  def apply[Node, Codec <: MessageCodec[Node], Effect[_], Context, Adapter](
    transport: EndpointTransport[Effect, Context, Adapter],
    rpcProtocol: RpcProtocol[Node, Codec, Context],
  ): Endpoint[Node, Codec, Effect, Context, Adapter] = {
    val handler = BindingHandler(rpcProtocol, transport.effectSystem, ListMap.empty)
    Endpoint(transport, rpcProtocol, handler, handler.functions)
  }

  /**
   * Creates an RPC client builder with specified effect transport plugin.
   *
   * @param transport
   *   transport layer transport plugin
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   RPC message context type
   * @tparam Adapter
   *   transport layer transport type
   * @return
   *   RPC client builder
   */
  def transport[Effect[_], Context, Adapter](
    transport: EndpointTransport[Effect, Context, Adapter]
  ): EndpointBuilder[Effect, Context, Adapter] =
    EndpointBuilder(transport)
}
