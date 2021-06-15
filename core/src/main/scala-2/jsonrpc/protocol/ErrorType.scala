package jsonrpc.protocol

/** JSON-RPC error types with codes. */
object ErrorType extends Enumeration {
  type ErrorType = ErrorTypeValue
  sealed case class ErrorTypeValue private[ErrorType](name: String, code: Int) extends Val(name)

  val ParseError = ErrorTypeValue("ParseError", -32700)
  val InvalidRequest = ErrorTypeValue("InvalidRequest", -32600)
  val MethodNotFound = ErrorTypeValue("MethodNotFound", -32601)
  val InvalidParams = ErrorTypeValue("InvalidParams", -32602)
  val InternalError = ErrorTypeValue("InternalError", -32603)
  val IOError = ErrorTypeValue("IOError", -32000)
  val ApplicationError = ErrorTypeValue("ApplicationError", 0)
}
