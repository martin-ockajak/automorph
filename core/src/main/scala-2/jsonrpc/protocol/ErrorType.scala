package jsonrpc.protocol

/** JSON-RPC error types with codes. */
sealed abstract class ErrorType(val name: String, val code: Int)

object ErrorType {
  case object ParseError extends ErrorType("ParseError", -32700)
  case object InvalidRequest extends ErrorType("InvalidRequest", -32600)
  case object MethodNotFound extends ErrorType("MethodNotFound", -32601)
  case object InvalidParams extends ErrorType("InvalidParams", -32602)
  case object InternalError extends ErrorType("InternalError", -32603)
  case object IOError extends ErrorType("IOError", -32000)
  case object ApplicationError extends ErrorType("ApplicationError", 0)
}
