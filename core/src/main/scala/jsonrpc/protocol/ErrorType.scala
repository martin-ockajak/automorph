package jsonrpc.protocol

/** JSON-RPC error types with codes. */
sealed abstract class ErrorType(val code: Int) {
  def name: String = toString
}

object ErrorType {
  case object ParseError extends ErrorType(-32700)
  case object InvalidRequest extends ErrorType(-32600)
  case object MethodNotFound extends ErrorType(-32601)
  case object InvalidParams extends ErrorType(-32602)
  case object InternalError extends ErrorType(-32603)
  case object IOError extends ErrorType(-32000)
  case object ApplicationError extends ErrorType(0)
}
