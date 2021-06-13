package jsonrpc.protocol

/** JSON-RPC error types with codes. */
object ErrorType extends Enumeration {
  type ErrorType = ErrorType
  sealed case class ErrorType private[ErrorType](name: String, code: Int) extends Val(name)

  case ParseError extends ErrorType("ParseError", -32700)
  case InvalidRequest extends ErrorType("InvalidRequest", -32600)
  case MethodNotFound extends ErrorType("MethodNotFound", -32601)
  case InvalidParams extends ErrorType("InvalidParams", -32602)
  case InternalError extends ErrorType("InternalError", -32603)
  case IOError extends ErrorType("IOError", -32000)
  case ApplicationError extends ErrorType("ApplicationError", 0)
}
