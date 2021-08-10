package automorph.protocol

import automorph.protocol.JsonRpcProtocol.{defaultErrorToException, defaultExceptionToError}
import automorph.protocol.jsonrpc.{ErrorMapping, ErrorType, JsonRpcCore, Message}
import automorph.spi.{MessageCodec, RpcProtocol}

/**
 * JSON-RPC protocol plugin.
 *
 * @constructor Creates a JSON-RPC protocol plugin.
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param codec message codec plugin
 * @param errorToException maps a JSON-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding JSON-RPC error
 * @param encodeMessage coverts a JSON-RPC message to message format node
 * @param decodeMessage coverts a message format node to JSON-RPC message
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

case object JsonRpcProtocol extends ErrorMapping:

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
  inline def apply[Node, Codec <: MessageCodec[Node]](
    codec: Codec,
    errorToException: (String, Int) => Throwable = defaultErrorToException,
    exceptionToError: Throwable => ErrorType = defaultExceptionToError
  ): JsonRpcProtocol[Node, Codec] =
    val encodeMessage = (message: Message[Node]) => codec.encode[Message[Node]](message)
    val decodeMessage = (node: Node) => codec.decode[Message[Node]](node)
    val encodeStrings = (value: List[String]) => codec.encode[List[String]](value)
    JsonRpcProtocol(codec, errorToException, exceptionToError, encodeMessage, decodeMessage, encodeStrings)
