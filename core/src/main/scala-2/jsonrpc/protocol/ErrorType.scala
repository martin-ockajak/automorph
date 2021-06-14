package jsonrpc.protocol

/** JSON-RPC error types with codes. */
object ErrorType extends Enumeration {
  type ErrorType = ErrorType
  sealed case class ErrorType private[ErrorType](name: String, code: Int) extends Val(name)

  val ParseError = ErrorType("ParseError", -32700)
  val InvalidRequest = ErrorType("InvalidRequest", -32600)
  val MethodNotFound = ErrorType("MethodNotFound", -32601)
  val InvalidParams = ErrorType("InvalidParams", -32602)
  val InternalError = ErrorType("InternalError", -32603)
  val IOError = ErrorType("IOError", -32000)
  val ApplicationError = ErrorType("ApplicationError", 0)
}
