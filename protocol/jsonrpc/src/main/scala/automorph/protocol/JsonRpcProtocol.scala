package automorph.protocol

import automorph.protocol.JsonRpcProtocol.{defaultErrorToException, defaultExceptionToError}
import automorph.protocol.jsonrpc.{ErrorMapping, ErrorType, JsonRpcCore}
import automorph.spi.RpcProtocol.{FunctionNotFoundException, InvalidRequestException, InvalidResponseException}
import automorph.spi.{Message, MessageCodec, RpcProtocol}

/**
 * JSON-RPC 2.0 protocol implementation.
 *
 * @constructor Creates a JSON-RPC 2.0 protocol implementation.
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param codec message codec plugin
 * @param errorToException maps a JSON-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding JSON-RPC error
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class JsonRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  errorToException: (String, Int) => Throwable = defaultErrorToException,
  exceptionToError: Throwable => ErrorType = defaultExceptionToError
) extends JsonRpcCore[Node, Codec] with RpcProtocol[Node]


case object JsonRpcProtocol extends ErrorMapping
