package jsonrpc.protocol

import java.io.IOException
import jsonrpc.spi.{MessageError, Message}

/**
 * JSON-RPC protocol data structures.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 */
case object Errors:

  /** JSON-RPC parse error. */
  final case class ParseError(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC invalid request error. */
  final case class InvalidRequest(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC method not found error. */
  final case class MethodNotFound(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** JSON-RPC internal error. */
  final case class InternalError(
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
    classOf[ParseError] -> ErrorType.ParseError,
    classOf[InvalidRequest] -> ErrorType.InvalidRequest,
    classOf[MethodNotFound] -> ErrorType.MethodNotFound,
    classOf[IllegalArgumentException] -> ErrorType.InvalidParams,
    classOf[InternalError] -> ErrorType.InternalError,
    classOf[IOException] -> ErrorType.IOError
  ).withDefaultValue(ErrorType.ApplicationError)

  /** Mapping of JSON-RPC errors to standard exception types. */
  def errorException(code: Int, message: String): Throwable = code match
    case ErrorType.ParseError.code                   => ParseError(message, None.orNull)
    case ErrorType.InvalidRequest.code               => InvalidRequest(message, None.orNull)
    case ErrorType.MethodNotFound.code               => MethodNotFound(message, None.orNull)
    case ErrorType.InvalidParams.code                => IllegalArgumentException(message, None.orNull)
    case ErrorType.InternalError.code                => InternalError(message, None.orNull)
    case ErrorType.IOError.code                      => IOException(message, None.orNull)
    case _ if code < ErrorType.ApplicationError.code => InternalError(message, None.orNull)
    case _                                           => RuntimeException(message, None.orNull)

  /**
   * Return specified mandatory property value or throw an exception if it is missing.
   *
   * @param value property value
   * @param name property name
   * @tparam T property type
   * @return property value
   * @throws InvalidRequest if the property value is missing
   */
  def mandatory[T](value: Option[T], name: String): T = value.getOrElse(
    throw InvalidRequest(s"Missing message property: $name", None.orNull)
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
    LazyList.iterate(Option(throwable))(_.flatMap(error => Option(error.getCause)))
      .takeWhile(_.isDefined).flatten.filter(filter).take(maxCauses).map { throwable =>
      val exceptionName = throwable.getClass.getSimpleName
      val message = Option(throwable.getMessage).getOrElse("")
      s"[$exceptionName] $message"
    }
