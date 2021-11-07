package automorph.protocol

import automorph.protocol.jsonrpc.{ErrorMapping, ErrorType, JsonRpcCore, Message}
import automorph.specification.{OpenApi, OpenRpc}
import automorph.spi.{MessageCodec, RpcProtocol}

/**
 * JSON-RPC protocol plugin.
 *
 * Provides the following JSON-RPC methods for service discovery:
 * - `rpc.discover` - API specification in OpenRPC format
 * - `api.discover` - API specification in OpenAPI format
 *
 * @constructor Creates a JSON-RPC protocol plugin.
 * @see [[https://www.jsonrpc.org/specification Protocol specification]]
 * @param codec message codec plugin
 * @param mapError maps a JSON-RPC error to a corresponding exception
 * @param mapException maps an exception to a corresponding JSON-RPC error
 * @param namedArguments if true, pass arguments by name, if false pass arguments by position
 * @param mapOpenRpc transforms generated OpenRPC specification
 * @param mapOpenApi transforms generated OpenAPI specification
 * @param encodeMessage converts a JSON-RPC message to message format node
 * @param decodeMessage converts a message format node to JSON-RPC message
 * @param encodeStrings converts list of strings to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Context message context type
 */
final case class JsonRpcProtocol[Node, Codec <: MessageCodec[Node], Context](
  codec: Codec,
  mapError: (String, Int) => Throwable,
  mapException: Throwable => ErrorType,
  namedArguments: Boolean,
  mapOpenApi: OpenApi => OpenApi,
  mapOpenRpc: OpenRpc => OpenRpc,
  protected val encodeMessage: Message[Node] => Node,
  protected val decodeMessage: Node => Message[Node],
  protected val encodeStrings: List[String] => Node
) extends JsonRpcCore[Node, Codec, Context] with RpcProtocol[Node, Codec, Context]

object JsonRpcProtocol extends ErrorMapping:

  /** Service discovery method providing API specification in OpenRPC format. */
  val openRpcFunction: String = "rpc.discover"
  /** Service discovery method providing API specification in OpenAPI format. */
  val openApiFunction: String = "api.discover"

  /**
   * Creates a JSON-RPC protocol plugin.
   *
   * Provides the following JSON-RPC methods for service discovery:
   * - `rpc.discover` - API specification in OpenRPC format
   * - `api.discover` - API specification in OpenAPI format
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param mapError maps a JSON-RPC error to a corresponding exception
   * @param mapException maps an exception to a corresponding JSON-RPC error
   * @param namedArguments if true, pass arguments by name, if false pass arguments by position
   * @param mapOpenRpc transforms generated OpenRPC specification
   * @param mapOpenApi transforms generated OpenAPI specification
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return JSON-RPC protocol plugin
   */
  inline def apply[Node, Codec <: MessageCodec[Node], Context](
    codec: Codec,
    mapError: (String, Int) => Throwable = defaultMapError,
    mapException: Throwable => ErrorType = defaultMapException,
    namedArguments: Boolean = true,
    mapOpenApi: OpenApi => OpenApi = identity,
    mapOpenRpc: OpenRpc => OpenRpc = identity
  ): JsonRpcProtocol[Node, Codec, Context] =
    val encodeMessage = (message: Message[Node]) => codec.encode[Message[Node]](message)
    val decodeMessage = (node: Node) => codec.decode[Message[Node]](node)
    val encodeStrings = (value: List[String]) => codec.encode[List[String]](value)
    JsonRpcProtocol(
      codec,
      mapError,
      mapException,
      namedArguments,
      mapOpenApi,
      mapOpenRpc,
      encodeMessage,
      decodeMessage,
      encodeStrings
    )
