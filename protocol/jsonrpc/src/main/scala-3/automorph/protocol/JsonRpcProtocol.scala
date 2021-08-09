package automorph.protocol

import automorph.protocol.JsonRpcProtocol.{defaultErrorToException, defaultExceptionToError}
import automorph.protocol.jsonrpc.{ErrorMapping, ErrorType, JsonRpcCore}
import automorph.spi.{MessageCodec, RpcProtocol}

/**
 * JSON-RPC protocol plugin.
 *
 * @constructor Creates a JSON-RPC protocol plugin.
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param codec message codec plugin
 * @param errorToException maps a JSON-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding JSON-RPC error
 * @param encodeStrings converts list of strings to message codec node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class JsonRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  errorToException: (String, Int) => Throwable,
  exceptionToError: Throwable => ErrorType,
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
     val encodeStrings = (value: List[String]) => codec.encode[List[String]](value)
     JsonRpcProtocol(codec, errorToException, exceptionToError, encodeStrings)
