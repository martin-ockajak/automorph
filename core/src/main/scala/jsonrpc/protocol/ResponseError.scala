package jsonrpc.protocol

import java.io.IOException
import jsonrpc.protocol.ErrorType
import jsonrpc.spi.MessageError


/**
 * JSON-RPC call response error.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param code error code
 * @param message error description
 * @param data additional error information
 * @tparam Node message node representation type
 */
private[jsonrpc] final case class ResponseError[Node](
  code: Int,
  message: String,
  data: Option[Node]
) {

  def formed: MessageError[Node] = MessageError[Node](
    code = Some(code),
    message = Some(message),
    data = data
  )
}

case object ResponseError {

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

  private[jsonrpc] def apply[Node](error: MessageError[Node]): ResponseErrorx[Node] = {
    val code = mandatory(error.code, "code")
    val message = mandatory(error.message, "message")
    new ResponseErrorx(code, message, error.data)
  }

  /**
   * Assemble detailed trace of an exception and its causes.
   *
   * @param throwable exception
   * @param maxCauses maximum number of included exception causes
   * @return error messages
   */
  private[jsonrpc] def trace(throwable: Throwable, maxCauses: Int = 100): Seq[String] =
    LazyList.iterate(Option(throwable))(_.flatMap(error => Option(error.getCause)))
      .takeWhile(_.isDefined).flatten.take(maxCauses).map { throwable =>
      val exceptionName = throwable.getClass.getSimpleName
      val message = Option(throwable.getMessage).getOrElse("")
      s"[$exceptionName] $message"
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
    throw InvalidRequestException(s"Missing message property: $name", None.orNull)
  )
}
