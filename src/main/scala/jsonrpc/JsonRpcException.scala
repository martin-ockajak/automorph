package jsonrpc

/**
 * JSON-RPC error.
 *
 * An API method implementation exposed via JSON-RPC can throw this error to return specific JSON-RPC error details.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC error.
 * @param message error description
 * @param data additional error information
 * @param cause error cause
 * @tparam Node data format node representation type
 */
final case class JsonRpcException[Node](
  message: String,
  data: Node,
  cause: Throwable
) extends RuntimeException(message, cause)
