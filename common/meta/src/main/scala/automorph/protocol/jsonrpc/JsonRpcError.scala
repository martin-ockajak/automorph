package automorph.protocol.jsonrpc

/**
 * JSON-RPC API error exception.
 *
 * API methods bound via a JSON-RPC handler can throw this exception to customize JSON-RPC error response data.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a new JSON-RPC error exception.
 * @param message error description
 * @param code error code
 * @param data additional error information
 * @param cause exception cause
 * @tparam Node message format node representation type
 */
final case class JsonRpcError[Node](
  message: String,
  code: Int,
  data: Option[Node],
  cause: Throwable
) extends RuntimeException(message, cause)
