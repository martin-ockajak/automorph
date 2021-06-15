package jsonrpc.protocol

class ErrorExceptions {

  /** JSON-RPC parse error. */
  final case class ParseError(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC invalid request error. */
  final case class InvalidRequest(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC method not found error. */
  final case class MethodNotFound(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC internal error. */
  final case class InternalError(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)
}
