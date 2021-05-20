package jsonrpc

/**
 * JSON-RPC error.
 * An API call resulting in this exception translates into the specific JSON-RPC error details.
 *
 * @param message error description
 * @param data additional error information
 * @param cause error cause
 * @tparam Node data format node representation type
 */
final case class JsonRpcError[Node](
  message: String,
  data: Node,
  cause: Throwable
) extends RuntimeException(message, cause)
