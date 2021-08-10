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
 * @param errorToException maps a JSON-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding JSON-RPC error
 * @param encodeMessage converts a JSON-RPC message to message format node
 * @param decodeMessage converts a message format node to JSON-RPC message
 * @param encodeStrings converts list of strings to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class JsonRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  errorToException: (String, Int) => Throwable,
  exceptionToError: Throwable => ErrorType,
  protected val encodeMessage: Message[Node] => Node,
  protected val decodeMessage: Node => Message[Node],
  protected val encodeStrings: List[String] => Node
) extends JsonRpcCore[Node, Codec] with RpcProtocol[Node]

case object JsonRpcProtocol extends ErrorMapping {
  /**
   * Creates a JSON-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param errorToException maps a JSON-RPC error to a corresponding exception
   * @param exceptionToError maps an exception to a corresponding JSON-RPC error
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return JSON-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node]](
    codec: Codec,
    errorToException: (String, Int) => Throwable = defaultErrorToException,
    exceptionToError: Throwable => ErrorType = defaultExceptionToError
  ): JsonRpcProtocol[Node, Codec] =
    macro applyMacro[Node, Codec]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]: c.WeakTypeTag](c: blackbox.Context)(
    codec: c.Expr[Codec],
    errorToException: c.Expr[(String, Int) => Throwable],
    exceptionToError: c.Expr[Throwable => ErrorType]
  ): c.Expr[JsonRpcProtocol[Node, Codec]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[Any](q"""
      new automorph.protocol.JsonRpcProtocol(
        $codec,
        $errorToException,
        $exceptionToError,
        message => codec.encode[automorph.protocol.jsonrpc.Message[Node]](message),
        node => codec.decode[automorph.protocol.jsonrpc.Message[Node]](node),
        value => $codec.encode[List[String]](value)
      )
    """).asInstanceOf[c.Expr[JsonRpcProtocol[Node, Codec]]]
  }
}
