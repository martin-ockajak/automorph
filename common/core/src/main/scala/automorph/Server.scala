package automorph

import automorph.handler.BindingHandler
import automorph.server.meta.ServerMeta
import automorph.spi.protocol.RpcFunction
import automorph.spi.{MessageCodec, RequestHandler, RpcProtocol, ServerTransport}
import scala.collection.immutable.ListMap

/**
 * RPC server.
 *
 * Used to serve remote API requests using specific message transport protocol and invoke bound API
 * methods to process them.
 *
 * Automatically derives remote API bindings for existing API instances.
 *
 * @constructor
 *   Creates a RPC server with specified protocol and transport plugins supporting corresponding message context type.
 * @param rpcProtocol
 *   RPC protocol plugin
 * @param transport
 *   server message transport plugin
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
 */
final case class Server[Node, Codec <: MessageCodec[Node], Effect[_], Context] (
  transport: ServerTransport[Effect, Context],
  rpcProtocol: RpcProtocol[Node, Codec, Context],
  handler: RequestHandler[Effect, Context],
  functions: Seq[RpcFunction] = Seq.empty,
) extends ServerMeta[Node, Codec, Effect, Context] {

  private val registeredTransport = transport.clone(handler)

  /**
   * Starts this server to process incoming requests.
   *
   * @return
   *   active RPC server
   */
  def init(): Effect[Server[Node, Codec, Effect, Context]] =
    registeredTransport.effectSystem.map(transport.init())(_ => this)

  /**
   * Stops this server freeing the underlying resources.
   *
   * @return
   *   passive RPC server
   */
  def close(): Effect[Server[Node, Codec, Effect, Context]] =
    registeredTransport.effectSystem.map(transport.close())(_ => this)

  override def toString: String = {
    val plugins = Map[String, Any](
      "rpcProtocol" -> rpcProtocol,
      "transport" -> registeredTransport,
    ).map { case (name, plugin) =>
      s"$name = ${plugin.getClass.getName}"
    }.mkString(", ")
    s"${this.getClass.getName}($plugins)"
  }
}

object Server {

  /**
   * RPC server builder.
   *
   * @constructor
   *   Creates a new RPC server builder.
   * @param transport
   *   message transport plugin
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   RPC message context type
   */
  case class ServerBuilder[Effect[_], Context](transport: ServerTransport[Effect, Context]) {

    /**
     * Creates a new RPC server with specified RPC protocol plugin.
     *
     * @param rpcProtocol
     *   RPC protocol plugin
     * @tparam Node
     *   message node type
     * @tparam Codec
     *   message codec plugin type
     * @return
     *   RPC server builder
     */
    def rpcProtocol[Node, Codec <: MessageCodec[Node]](
      rpcProtocol: RpcProtocol[Node, Codec, Context]
    ): Server[Node, Codec, Effect, Context] =
      Server(transport, rpcProtocol)
  }

  /**
   * Creates a RPC server with specified protocol and transport plugins supporting corresponding message context type.
   *
   * @param transport
   *   server message transport plugin
   * @param protocol
   *   RPC protocol plugin
   * @tparam Node
   *   message node type
   * @tparam Codec
   *   message codec plugin type
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   RPC message context type
   * @return RPC server
   */
  def apply[Node, Codec <: MessageCodec[Node], Effect[_], Context](
    transport: ServerTransport[Effect, Context],
    protocol: RpcProtocol[Node, Codec, Context],
  ): Server[Node, Codec, Effect, Context] = {
    val handler = BindingHandler(transport.effectSystem, protocol, ListMap.empty)
    Server(transport, protocol, handler, handler.functions)
  }

  /**
   * Creates an RPC client builder with specified effect transport plugin.
   *
   * @param transport
   *   message transport plugin
   * @tparam Effect
   *   effect type
   * @tparam Context
   *   RPC message context type
   * @return
   *   RPC client builder
   */
  def transport[Effect[_], Context](transport: ServerTransport[Effect, Context]): ServerBuilder[Effect, Context] =
    ServerBuilder(transport)
}
