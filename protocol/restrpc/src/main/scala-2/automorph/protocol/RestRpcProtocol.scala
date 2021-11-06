package automorph.protocol

import automorph.protocol.restrpc.{ErrorMapping, Message, RestRpcCore}
import automorph.spi.{MessageCodec, RpcProtocol}
import automorph.transport.http.HttpContext
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * REST-RPC protocol plugin.
 *
 * @constructor Creates a REST-RPC protocol plugin.
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @param codec message codec plugin
 * @param pathPrefix API path prefix
 * @param mapError maps a REST-RPC error to a corresponding exception
 * @param mapException maps an exception to a corresponding REST-RPC error
 * @param discovery if true, provides OpenAPI specification via 'api.discover' API function
 * @param encodeRequest converts a REST-RPC request to message format node
 * @param decodeRequest converts a message format node to REST-RPC request
 * @param encodeResponse converts a REST-RPC response to message format node
 * @param decodeResponse converts a message format node to REST-RPC response
 * @param encodeStrings converts list of strings to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Context message context type
 */
final case class RestRpcProtocol[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](
  codec: Codec,
  pathPrefix: String,
  mapError: (String, Option[Int]) => Throwable = RestRpcProtocol.defaultMapError,
  mapException: Throwable => Option[Int] = RestRpcProtocol.defaultMapException,
  discovery: Boolean,
  protected val encodeRequest: Message.Request[Node] => Node,
  protected val decodeRequest: Node => Message.Request[Node],
  protected val encodeResponse: Message[Node] => Node,
  protected val decodeResponse: Node => Message[Node],
  protected val encodeStrings: List[String] => Node
) extends RestRpcCore[Node, Codec, Context] with RpcProtocol[Node, Codec, Context]

object RestRpcProtocol extends ErrorMapping {

  /**
   * Creates a REST-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification REST-RPC protocol specification]]
   * @param codec message codec plugin
   * @param pathPrefix API path prefix
   * @param mapError maps a REST-RPC error to a corresponding exception
   * @param mapException maps an exception to a corresponding REST-RPC error
   * @param discovery if true, provides OpenAPI specification via 'api.discover' API function
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return REST-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](
    codec: Codec,
    pathPrefix: String,
    mapError: (String, Option[Int]) => Throwable,
    mapException: Throwable => Option[Int],
    discovery: Boolean
  ): RestRpcProtocol[Node, Codec, Context] =
    macro applyMacro[Node, Codec, Context]

  /**
   * Creates a REST-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification REST-RPC protocol specification]]
   * @param codec message codec plugin
   * @param pathPrefix API path prefix
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return REST-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](
    codec: Codec,
    pathPrefix: String
  ): RestRpcProtocol[Node, Codec, Context] =
    macro applyDefaultsMacro[Node, Codec, Context]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node], Context <: HttpContext[_]](c: blackbox.Context)(
    codec: c.Expr[Codec],
    pathPrefix: c.Expr[String],
    mapError: c.Expr[(String, Option[Int]) => Throwable],
    mapException: c.Expr[Throwable => Option[Int]],
    discovery: c.Expr[Boolean]
  ): c.Expr[RestRpcProtocol[Node, Codec, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[RestRpcProtocol[Node, Codec, Context]](q"""
      new automorph.protocol.RestRpcProtocol(
        $codec,
        $pathPrefix,
        $mapError,
        $mapException,
        $discovery,
        request => $codec.encode[automorph.protocol.restrpc.Message.Request[${weakTypeOf[Node]}]](request),
        node => $codec.decode[automorph.protocol.restrpc.Message.Request[${weakTypeOf[Node]}]](node),
        response => $codec.encode[automorph.protocol.restrpc.Message[${weakTypeOf[Node]}]](response),
        node => $codec.decode[automorph.protocol.restrpc.Message[${weakTypeOf[Node]}]](node),
        value => $codec.encode[List[String]](value)
      )
    """)
  }

  def applyDefaultsMacro[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](c: blackbox.Context)(
    codec: c.Expr[Codec],
    pathPrefix: c.Expr[String]
  ): c.Expr[RestRpcProtocol[Node, Codec, Context]] = {
    import c.universe.Quasiquote

    c.Expr[RestRpcProtocol[Node, Codec, Context]](q"""
      automorph.protocol.RestRpcProtocol(
        $codec,
        $pathPrefix,
        automorph.protocol.RestRpcProtocol.defaultMapError,
        automorph.protocol.RestRpcProtocol.defaultMapException,
        true
      )
    """)
  }
}
