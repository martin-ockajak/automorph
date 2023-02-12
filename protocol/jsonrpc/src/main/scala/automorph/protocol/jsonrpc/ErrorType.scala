package automorph.protocol.jsonrpc

/**
 * JSON-RPC error type with code.
 *
 * @see
 *   [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 */
sealed abstract class ErrorType(val code: Int) {

  def name: String =
    toString
}

/**
 * JSON-RPC error types with codes.
 *
 * @see
 *   [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 */
object ErrorType {

  /** JSON-RPC parse error. */
  final case class ParseErrorException(message: String, cause: Throwable = None.orNull)
    extends RuntimeException(message, cause)

  /** JSON-RPC internal error. */
  final case class InternalErrorException(message: String, cause: Throwable = None.orNull)
    extends RuntimeException(message, cause)

  /** JSON-RPC sever error. */
  final case class ServerErrorException(message: String, cause: Throwable = None.orNull)
    extends RuntimeException(message, cause)

  case object ParseError extends ErrorType(-32700)

  case object InvalidRequest extends ErrorType(-32600)

  case object MethodNotFound extends ErrorType(-32601)

  case object InvalidParams extends ErrorType(-32602)

  case object InternalError extends ErrorType(-32603)

  case object ServerError extends ErrorType(-32000)

  case object ReservedError extends ErrorType(-32768)

  case object ApplicationError extends ErrorType(0)
}
