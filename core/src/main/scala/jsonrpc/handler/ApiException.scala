package jsonrpc.handler

/**
 * JSON-RPC exception.
 *
 * API methods bound via a JSON-RPC handler can throw this exception to customize JSON-RPC error response content.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC error.
 * @param message error description
 * @param data additional error information
 * @param cause exception cause
 * @tparam Node message format node representation type
 */
final case class ApiException[Node](
  message: String,
  data: Node,
  cause: Throwable
) extends RuntimeException(message, cause)
