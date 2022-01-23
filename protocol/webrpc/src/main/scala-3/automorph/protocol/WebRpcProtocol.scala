package automorph.protocol

import automorph.protocol.webrpc.{ErrorMapping, Message, WebRpcCore}
import automorph.schema.OpenApi
import automorph.spi.{MessageCodec, RpcProtocol}
import automorph.transport.http.HttpContext

/**
 * Web-RPC protocol implementation.
 *
 * Provides the following Web-RPC functions for service discovery:
 * - `api.discover` - API description in OpenAPI format
 *
 * @constructor Creates a Web-RPC 2.0 protocol implementation.
 * @see [[https://automorph.org/rest-rpc Protocol specification]]
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
final case class WebRpcProtocol[Node, Codec <: MessageCodec[Node], Context <: HttpContext[?]](
  codec: Codec,
  pathPrefix: String,
  mapError: (String, Option[Int]) => Throwable,
  mapException: Throwable => Option[Int],
  mapOpenApi: OpenApi => OpenApi,
  protected val encodeRequest: Message.Request[Node] => Node,
  protected val decodeRequest: Node => Message.Request[Node],
  protected val encodeResponse: Message[Node] => Node,
  protected val decodeResponse: Node => Message[Node],
  protected val encodeOpenApi: OpenApi => Node,
  protected val encodeString: String => Node
) extends WebRpcCore[Node, Codec, Context] with RpcProtocol[Node, Codec, Context]

object WebRpcProtocol extends ErrorMapping:

  /** Service discovery method providing API description in OpenAPI format. */
  val openApiFunction: String = "api.discover"

  /**
   * Creates a Web-RPC protocol plugin.
   *
   * Provides the following Web-RPC functions for service discovery:
   * - `api.discover` - API description in OpenAPI format
   *
   * @see [[https://automorph.org/rest-rpc Web-RPC protocol specification]]
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
  inline def apply[Node, Codec <: MessageCodec[Node], Context <: HttpContext[?]](
    codec: Codec,
    pathPrefix: String,
    mapError: (String, Option[Int]) => Throwable = defaultMapError,
    mapException: Throwable => Option[Int] = defaultMapException,
    mapOpenApi: OpenApi => OpenApi = identity
  ): WebRpcProtocol[Node, Codec, Context] =
    val encodeRequest = (value: Message.Request[Node]) => codec.encode[Message.Request[Node]](value)
    val decodeRequest = (requestNode: Node) => codec.decode[Message.Request[Node]](requestNode)
    val encodeResponse = (value: Message[Node]) => codec.encode[Message[Node]](value)
    val decodeResponse = (responseNode: Node) => codec.decode[Message[Node]](responseNode)
    val encodeOpenApi = (openApi: OpenApi) => codec.encode[OpenApi](openApi)
    val encodeString = (string: String) => codec.encode[String](string)
    WebRpcProtocol(
      codec,
      pathPrefix,
      mapError,
      mapException,
      mapOpenApi,
      encodeRequest,
      decodeRequest,
      encodeResponse,
      decodeResponse,
      encodeOpenApi,
      encodeString
    )
