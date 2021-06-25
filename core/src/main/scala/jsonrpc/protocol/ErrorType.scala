package jsonrpc.protocol

import java.io.IOException
import jsonrpc.protocol.ResponseError.{InternalErrorException, InvalidRequestException, MethodNotFoundException, ParseErrorException}

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

  /** Mapping of standard exception types to JSON-RPC errors. */
  val fromException: Map[Class[_ <: Throwable], ErrorType] = Map(
    classOf[ParseErrorException] -> ErrorType.ParseError,
    classOf[InvalidRequestException] -> ErrorType.InvalidRequest,
    classOf[MethodNotFoundException] -> ErrorType.MethodNotFound,
    classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
    classOf[InternalErrorException] -> ErrorType.InternalError,
    classOf[IOException] -> ErrorType.IOError
  ).withDefaultValue(ErrorType.ApplicationError).asInstanceOf[Map[Class[_ <: Throwable], ErrorType]]

  /** Mapping of JSON-RPC errors to standard exception types. */
  def toException(code: Int, message: String): Throwable = code match {
    case ErrorType.ParseError.code                   => ParseErrorException(message, None.orNull)
    case ErrorType.InvalidRequest.code               => InvalidRequestException(message, None.orNull)
    case ErrorType.MethodNotFound.code               => MethodNotFoundException(message, None.orNull)
    case ErrorType.InvalidParams.code                => new IllegalArgumentException(message, None.orNull)
    case ErrorType.InternalError.code                => InternalErrorException(message, None.orNull)
    case ErrorType.IOError.code                      => new IOException(message, None.orNull)
    case _ if code < ErrorType.ApplicationError.code => InternalErrorException(message, None.orNull)
    case _                                           => new RuntimeException(message, None.orNull)
  }
}
