package automorph.protocol

import automorph.protocol.restrpc.{ErrorMapping, Message, RestRpcCore}
import automorph.description.OpenApi
import automorph.spi.{MessageCodec, RpcProtocol}
import automorph.transport.http.HttpContext

/**
 * REST-RPC protocol implementation.
 *
 * Provides the following REST-RPC functions for service discovery:
 * - `api.discover` - API description in OpenAPI format
 *
 * @constructor Creates a REST-RPC 2.0 protocol implementation.
 * @see [[https://automorph.org/rest-rpc Protocol specification]]
 * @param codec message codec plugin
 * @param pathPrefix API path prefix
 * @param mapError maps a REST-RPC error to a corresponding exception
 * @param mapException maps an exception to a corresponding REST-RPC error
 * @param mapOpenApi transforms generated OpenAPI description
 * @param encodeRequest converts a REST-RPC request to message format node
 * @param decodeRequest converts a message format node to REST-RPC request
 * @param encodeResponse converts a REST-RPC response to message format node
 * @param decodeResponse converts a message format node to REST-RPC response
 * @param encodeOpenApi converts an OpenAPI description to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Context message context type
 */
final case class RestRpcProtocol[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](
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
) extends RestRpcCore[Node, Codec, Context] with RpcProtocol[Node, Codec, Context]

object RestRpcProtocol extends ErrorMapping:

  /** Service discovery method providing API description in OpenAPI format. */
  val openApiFunction: String= "api.discover"

  /**
   * Creates a REST-RPC protocol plugin.
   *
   * Provides the following REST-RPC functions for service discovery:
   * - `api.discover` - API description in OpenAPI format
   *
   * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
   * @param codec message codec plugin
   * @param pathPrefix API path prefix
   * @param mapError maps a REST-RPC error to a corresponding exception
   * @param mapException maps an exception to a corresponding REST-RPC error
   * @param mapOpenApi transforms generated OpenAPI description
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return REST-RPC protocol plugin
   */
  inline def apply[Node, Codec <: MessageCodec[Node], Context <: HttpContext[_]](
    codec: Codec,
    pathPrefix: String,
    mapError: (String, Option[Int]) => Throwable = defaultMapError,
    mapException: Throwable => Option[Int] = defaultMapException,
    mapOpenApi: OpenApi => OpenApi = identity
  ): RestRpcProtocol[Node, Codec, Context] =
    val encodeRequest = (request: Message.Request[Node]) => codec.encode[Message.Request[Node]](request)
    val decodeRequest = (node: Node) => codec.decode[Message.Request[Node]](node)
    val encodeResponse = (mesponse: Message[Node]) => codec.encode[Message[Node]](mesponse)
    val decodeResponse = (node: Node) => codec.decode[Message[Node]](node)
    val encodeOpenApi = (value: OpenApi) => codec.encode[OpenApi](value)
    RestRpcProtocol(
      codec,
      pathPrefix,
      mapError,
      mapException,
      mapOpenApi,
      encodeRequest,
      decodeRequest,
      encodeResponse,
      decodeResponse,
      encodeOpenApi
    )
