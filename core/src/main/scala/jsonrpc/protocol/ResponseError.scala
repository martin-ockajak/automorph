package jsonrpc.protocol

import java.io.IOException
import jsonrpc.protocol.ErrorType
import jsonrpc.protocol.ErrorType.mandatory
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

  private[jsonrpc] def apply[Node](error: MessageError[Node]): ResponseError[Node] = {
    val code = mandatory(error.code, "code")
    val message = mandatory(error.message, "message")
    new ResponseError(code, message, error.data)
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
}
