package jsonrpc.protocol

import java.io.IOException

/**
 * JSON-RPC error types with codes.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 */
sealed abstract class ErrorType(val code: Int) {
  def name: String = toString
}

/**
 * JSON-RPC error types with codes.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 */
object ErrorType {
  case object ParseError extends ErrorType(-32700)
  case object InvalidRequest extends ErrorType(-32600)
  case object MethodNotFound extends ErrorType(-32601)
  case object InvalidParams extends ErrorType(-32602)
  case object InternalError extends ErrorType(-32603)
  case object IOError extends ErrorType(-32000)
  case object ApplicationError extends ErrorType(0)

  /** JSON-RPC parse error. */
  final case class ParseErrorException(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC invalid request error. */
  final case class InvalidRequestException(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC method not found error. */
  final case class MethodNotFoundException(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC internal error. */
  final case class InternalErrorException(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

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

  /**
   * Return specified mandatory property value or throw an exception if it is missing.
   *
   * @param value property value
   * @param name property name
   * @tparam T property type
   * @return property value
   * @throws InvalidRequestException if the property value is missing
   */
  private[jsonrpc] def mandatory[T](value: Option[T], name: String): T = value.getOrElse(
    throw ErrorType.InvalidRequestException(s"Missing message property: $name", None.orNull)
  )
}
