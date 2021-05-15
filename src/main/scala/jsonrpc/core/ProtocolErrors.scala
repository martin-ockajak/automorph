package jsonrpc.core

import java.io.IOException

object ProtocolErrors {

  final case class ParseError(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  final case class InvalidRequest(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  enum ErrorType(val code: Int):
    case ParseError extends ErrorType(-32700)
    case InvalidRequest extends ErrorType(-32600)
    case MethodNotFound extends ErrorType(-32601)
    case InvalidParams extends ErrorType(-32602)
    case InternalError extends ErrorType(-32603)
    case IOError extends ErrorType(-32000)
    case ApplicationError extends ErrorType(0)

  lazy val exceptionErrors: Map[Class[?], ErrorType] = Map(
    classOf[ParseError] -> ErrorType.ParseError,
    classOf[InvalidRequest] -> ErrorType.InvalidRequest,
    classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
    classOf[IOException] -> ErrorType.IOError,
  )
}
