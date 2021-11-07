package automorph.protocol

import automorph.protocol.jsonrpc.{ErrorMapping, ErrorType, JsonRpcCore, Message}
import automorph.spi.{MessageCodec, RpcProtocol}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * JSON-RPC protocol implementation.
 *
 * Provides the following JSON-RPC methods for service discovery:
 * - `rpc.discover` - OpenRPC specification
 * - `api.discover` - OpenAPI specification
 *
 * @constructor Creates a JSON-RPC 2.0 protocol implementation.
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param codec message codec plugin
 * @param mapError maps a JSON-RPC error to a corresponding exception
 * @param mapException maps an exception to a corresponding JSON-RPC error
 * @param namedArguments if true, pass arguments by name, if false pass arguments by position
 * @param encodeMessage converts a JSON-RPC message to message format node
 * @param decodeMessage converts a message format node to JSON-RPC message
 * @param encodeStrings converts list of strings to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Context message context type
 */
final case class JsonRpcProtocol[Node, Codec <: MessageCodec[Node], Context](
  codec: Codec,
  mapError: (String, Int) => Throwable = JsonRpcProtocol.defaultMapError,
  mapException: Throwable => ErrorType = JsonRpcProtocol.defaultMapException,
  namedArguments: Boolean = true,
  protected val encodeMessage: Message[Node] => Node,
  protected val decodeMessage: Node => Message[Node],
  protected val encodeStrings: List[String] => Node
) extends JsonRpcCore[Node, Codec, Context] with RpcProtocol[Node, Codec, Context]

object JsonRpcProtocol extends ErrorMapping {

  /** Service discovery method providing OpenRPC specification. */
  val openRpcSpecFunction: String = "rpc.discover"
  /** Service discovery method providing OpenAPI specification. */
  val openApiSpecFunction: String = "api.discover"

  /**
   * Creates a JSON-RPC protocol plugin.
   *
   * Provides the following JSON-RPC methods for service discovery:
   * - `rpc.discover` - OpenRPC specification
   * - `api.discover` - OpenAPI specification
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param mapError maps a JSON-RPC error to a corresponding exception
   * @param mapException maps an exception to a corresponding JSON-RPC error
   * @param namedArguments if true, pass arguments by name, if false pass arguments by position
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return JSON-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node], Context](
    codec: Codec,
    mapError: (String, Int) => Throwable,
    mapException: Throwable => ErrorType,
    namedArguments: Boolean
  ): JsonRpcProtocol[Node, Codec, Context] =
    macro applyMacro[Node, Codec, Context]

  /**
   * Creates a JSON-RPC protocol plugin.
   *
   * Provides the following JSON-RPC methods for service discovery:
   * - `rpc.discover` - OpenRPC specification
   * - `api.discover` - OpenAPI specification
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return JSON-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node], Context](codec: Codec): JsonRpcProtocol[Node, Codec, Context] =
    macro applyDefaultsMacro[Node, Codec, Context]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node], Context](c: blackbox.Context)(
    codec: c.Expr[Codec],
    mapError: c.Expr[(String, Int) => Throwable],
    mapException: c.Expr[Throwable => ErrorType],
    namedArguments: c.Expr[Boolean]
  ): c.Expr[JsonRpcProtocol[Node, Codec, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[JsonRpcProtocol[Node, Codec, Context]](q"""
      new automorph.protocol.JsonRpcProtocol(
        $codec,
        $mapError,
        $mapException,
        $namedArguments,
        message => $codec.encode[automorph.protocol.jsonrpc.Message[${weakTypeOf[Node]}]](message),
        node => $codec.decode[automorph.protocol.jsonrpc.Message[${weakTypeOf[Node]}]](node),
        value => $codec.encode[List[String]](value)
      )
    """)
  }

  def applyDefaultsMacro[Node, Codec <: MessageCodec[Node], Context](c: blackbox.Context)(
    codec: c.Expr[Codec]
  ): c.Expr[JsonRpcProtocol[Node, Codec, Context]] = {
    import c.universe.Quasiquote

    c.Expr[JsonRpcProtocol[Node, Codec, Context]](q"""
      automorph.protocol.JsonRpcProtocol(
        $codec,
        automorph.protocol.JsonRpcProtocol.defaultMapError,
        automorph.protocol.JsonRpcProtocol.defaultMapException,
        true
      )
    """)
  }
}
