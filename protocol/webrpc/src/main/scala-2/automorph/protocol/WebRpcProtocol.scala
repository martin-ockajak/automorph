package automorph.protocol

import automorph.protocol.webrpc.{ErrorMapping, Message, WebRpcCore}
import automorph.schema.OpenApi
import automorph.spi.{MessageCodec, RpcProtocol}
import automorph.transport.http.HttpContext
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Web-RPC protocol plugin.
 *
 * Provides the following Web-RPC functions for service discovery:
 * - `api.discover` - API description in OpenAPI format
 *
 * @constructor Creates a Web-RPC protocol plugin.
 * @see [[https://automorph.org/rest-rpc Web-RPC protocol specification]]
 * @param codec message codec plugin
 * @param pathPrefix API path prefix
 * @param mapError maps a Web-RPC error to a corresponding exception
 * @param mapException maps an exception to a corresponding Web-RPC error
 * @param mapOpenApi transforms generated OpenAPI description
 * @param encodeRequest converts a Web-RPC request to message format node
 * @param decodeRequest converts a message format node to Web-RPC request
 * @param encodeResponse converts a Web-RPC response to message format node
 * @param decodeResponse converts a message format node to Web-RPC response
 * @param encodeOpenApi converts an OpenAPI description to message format node
 * @param encodeString converts a string to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Context message context type
 */
final case class WebRpcProtocol[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](
  codec: Codec,
  pathPrefix: String,
  mapError: (String, Option[Int]) => Throwable = WebRpcProtocol.defaultMapError,
  mapException: Throwable => Option[Int] = WebRpcProtocol.defaultMapException,
  mapOpenApi: OpenApi => OpenApi = identity,
  protected val encodeRequest: Message.Request[Node] => Node,
  protected val decodeRequest: Node => Message.Request[Node],
  protected val encodeResponse: Message[Node] => Node,
  protected val decodeResponse: Node => Message[Node],
  protected val encodeOpenApi: OpenApi => Node,
  protected val encodeString: String => Node
) extends WebRpcCore[Node, Codec, Context] with RpcProtocol[Node, Codec, Context]

object WebRpcProtocol extends ErrorMapping {

  /** Service discovery method providing API description in OpenAPI format. */
  val openApiFunction: String = "api.discover"

  /**
   * Creates a Web-RPC protocol plugin.
   *
   * Provides the following JSON-RPC functions for service discovery:
   * - `api.discover` - API description in OpenAPI format
   *
   * @see [[https://www.jsonrpc.org/specification Web-RPC protocol specification]]
   * @param codec message codec plugin
   * @param pathPrefix API path prefix
   * @param mapError maps a Web-RPC error to a corresponding exception
   * @param mapException maps an exception to a corresponding Web-RPC error
   * @param mapOpenApi transforms generated OpenAPI description
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return Web-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](
    codec: Codec,
    pathPrefix: String,
    mapError: (String, Option[Int]) => Throwable,
    mapException: Throwable => Option[Int],
    mapOpenApi: OpenApi => OpenApi
  ): WebRpcProtocol[Node, Codec, Context] =
    macro applyMacro[Node, Codec, Context]

  /**
   * Creates a Web-RPC protocol plugin.
   *
   * Provides the following Web-RPC functions for service discovery:
   * - `api.discover` - API description in OpenAPI format
   *
   * @see [[https://www.jsonrpc.org/specification Web-RPC protocol specification]]
   * @param codec message codec plugin
   * @param pathPrefix API path prefix
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return Web-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](
    codec: Codec,
    pathPrefix: String
  ): WebRpcProtocol[Node, Codec, Context] =
    macro applyDefaultsMacro[Node, Codec, Context]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node], Context <: HttpContext[_]](c: blackbox.Context)(
    codec: c.Expr[Codec],
    pathPrefix: c.Expr[String],
    mapError: c.Expr[(String, Option[Int]) => Throwable],
    mapException: c.Expr[Throwable => Option[Int]],
    mapOpenApi: c.Expr[OpenApi => OpenApi]
  ): c.Expr[WebRpcProtocol[Node, Codec, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[WebRpcProtocol[Node, Codec, Context]](q"""
      new automorph.protocol.WebRpcProtocol(
        $codec,
        $pathPrefix,
        $mapError,
        $mapException,
        $mapOpenApi,
        request => $codec.encode[automorph.protocol.webrpc.Message.Request[${weakTypeOf[Node]}]](request),
        requestNode => $codec.decode[automorph.protocol.webrpc.Message.Request[${weakTypeOf[Node]}]](requestNode),
        response => $codec.encode[automorph.protocol.webrpc.Message[${weakTypeOf[Node]}]](response),
        responseNode => $codec.decode[automorph.protocol.webrpc.Message[${weakTypeOf[Node]}]](responseNode),
        openApi => $codec.encode[automorph.schema.OpenApi](openApi),
        string => $codec.encode[String](string)
      )
    """)
  }

  def applyDefaultsMacro[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](c: blackbox.Context)(
    codec: c.Expr[Codec],
    pathPrefix: c.Expr[String]
  ): c.Expr[WebRpcProtocol[Node, Codec, Context]] = {
    import c.universe.Quasiquote

    c.Expr[WebRpcProtocol[Node, Codec, Context]](q"""
      automorph.protocol.WebRpcProtocol(
        $codec,
        $pathPrefix,
        automorph.protocol.WebRpcProtocol.defaultMapError,
        automorph.protocol.WebRpcProtocol.defaultMapException,
        identity
      )
    """)
  }
}
