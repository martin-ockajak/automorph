package automorph.protocol

import automorph.protocol.jsonrpc.{ErrorMapping, ErrorType, JsonRpcCore, Message}
import automorph.spi.{MessageCodec, RpcProtocol}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * JSON-RPC protocol implementation.
 *
 * @constructor Creates a JSON-RPC 2.0 protocol implementation.
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param codec message codec plugin
 * @param mapError maps a JSON-RPC error to a corresponding exception
 * @param mapException maps an exception to a corresponding JSON-RPC error
 * @param argumentsByName if true, pass arguments by name, if false pass arguments by position
 * @param encodeMessage converts a JSON-RPC message to message format node
 * @param decodeMessage converts a message format node to JSON-RPC message
 * @param encodeStrings converts list of strings to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class JsonRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  mapError: (String, Int) => Throwable = JsonRpcProtocol.defaultMapError,
  mapException: Throwable => ErrorType = JsonRpcProtocol.defaultMapException,
  argumentsByName: Boolean = true,
  protected val encodeMessage: Message[Node] => Node,
  protected val decodeMessage: Node => Message[Node],
  protected val encodeStrings: List[String] => Node
) extends JsonRpcCore[Node, Codec] with RpcProtocol[Node, Codec]

object JsonRpcProtocol extends ErrorMapping {
  /**
   * Creates a JSON-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param errorToException maps a JSON-RPC error to a corresponding exception
   * @param exceptionToError maps an exception to a corresponding JSON-RPC error
   * @param argumentsByName if true, pass arguments by name, if false pass arguments by position
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return JSON-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node]](
    codec: Codec,
    errorToException: (String, Int) => Throwable,
    exceptionToError: Throwable => ErrorType,
    argumentsByName: Boolean
  ): JsonRpcProtocol[Node, Codec] =
    macro applyMacro[Node, Codec]

  /**
   * Creates a JSON-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return JSON-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node]](codec: Codec): JsonRpcProtocol[Node, Codec] =
    macro applyDefaultsMacro[Node, Codec]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]](c: blackbox.Context)(
    codec: c.Expr[Codec],
    errorToException: c.Expr[(String, Int) => Throwable],
    exceptionToError: c.Expr[Throwable => ErrorType],
    argumentsByName: c.Expr[Boolean]
  ): c.Expr[JsonRpcProtocol[Node, Codec]] = {
    import c.universe.{Quasiquote, weakTypeOf}

    c.Expr[JsonRpcProtocol[Node, Codec]](q"""
      new automorph.protocol.JsonRpcProtocol(
        $codec,
        $errorToException,
        $exceptionToError,
        $argumentsByName,
        message => $codec.encode[automorph.protocol.jsonrpc.Message[${weakTypeOf[Node]}]](message),
        node => $codec.decode[automorph.protocol.jsonrpc.Message[${weakTypeOf[Node]}]](node),
        value => $codec.encode[List[String]](value)
      )
    """)
  }

  def applyDefaultsMacro[Node, Codec <: MessageCodec[Node]](c: blackbox.Context)(
    codec: c.Expr[Codec]
  ): c.Expr[JsonRpcProtocol[Node, Codec]] = {
    import c.universe.Quasiquote

    c.Expr[JsonRpcProtocol[Node, Codec]](q"""
      automorph.protocol.JsonRpcProtocol(
        $codec,
        automorph.protocol.JsonRpcProtocol.defaultErrorToException,
        automorph.protocol.JsonRpcProtocol.defaultExceptionToError,
        true
      )
    """)
  }
}
