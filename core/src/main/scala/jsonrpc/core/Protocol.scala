package jsonrpc.core

import java.io.IOException
import jsonrpc.spi.{MessageError, Message}
import jsonrpc.util.ValueOps.{asRight, asOption, asSome}

/**
 * JSON-RPC protocol data structures.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 */
case object Protocol:

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

  /** JSON-RPC error types with codes. */
  enum ErrorType(val code: Int):

    case ParseError extends ErrorType(-32700)
    case InvalidRequest extends ErrorType(-32600)
    case MethodNotFound extends ErrorType(-32601)
    case InvalidParams extends ErrorType(-32602)
    case InternalError extends ErrorType(-32603)
    case IOError extends ErrorType(-32000)
    case ApplicationError extends ErrorType(0)

  /** Mapping of standard exception types to JSON-RPC errors. */
  lazy val exceptionError: Map[Class[? <: Throwable], ErrorType] = Map(
    classOf[ParseErrorException] -> ErrorType.ParseError,
    classOf[InvalidRequestException] -> ErrorType.InvalidRequest,
    classOf[MethodNotFoundException] -> ErrorType.MethodNotFound,
    classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
    classOf[InternalErrorException] -> ErrorType.InternalError,
    classOf[IOException] -> ErrorType.IOError
  ).withDefaultValue(ErrorType.ApplicationError)

  /** Mapping of JSON-RPC errors to standard exception types. */
  def errorException(code: Int, message: String): Throwable = code match
    case ErrorType.ParseError.code                   => ParseErrorException(message, None.orNull)
    case ErrorType.InvalidRequest.code               => InvalidRequestException(message, None.orNull)
    case ErrorType.MethodNotFound.code               => MethodNotFoundException(message, None.orNull)
    case ErrorType.InvalidParams.code                => IllegalArgumentException(message, None.orNull)
    case ErrorType.InternalError.code                => InternalErrorException(message, None.orNull)
    case ErrorType.IOError.code                      => IOException(message, None.orNull)
    case _ if code < ErrorType.ApplicationError.code => InternalErrorException(message, None.orNull)
    case _                                           => RuntimeException(message, None.orNull)

  /**
   * Return specified mandatory property value or throw an exception if it is missing.
   *
   * @param value property value
   * @param name property name
   * @tparam T property type
   * @return property value
   * @throws InvalidRequestException if the property value is missing
   */
  def mandatory[T](value: Option[T], name: String): T = value.getOrElse(
    throw InvalidRequestException(s"Missing message property: $name", None.orNull)
  )

  /**
   * Assemble detailed error description from a throwable and its filtered causes.
   *
   * @param throwable exception
   * @param filter only include throwables satisfying this condition
   * @param maxCauses maximum number of included exception causes
   * @return error messages
   */
  def errorDetails(
    throwable: Throwable,
    filter: Throwable => Boolean = _ => true,
    maxCauses: Int = 100
  ): Seq[String] =
    LazyList.iterate(throwable.asSome)(_.flatMap(_.getCause.asOption))
      .takeWhile(_.isDefined).flatten.filter(filter).take(maxCauses).map { throwable =>
      val exceptionName = throwable.getClass.getSimpleName
      throwable.getMessage.asOption.map(_.trim).filter(_.nonEmpty).map { message =>
        s"$exceptionName: $message"
      }.getOrElse(exceptionName)
    }
