package automorph.protocol.jsonrpc

import automorph.protocol.jsonrpc.Response.mandatory
import automorph.spi.MessageError


/**
 * JSON-RPC call response error.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param message error description
 * @param code error code
 * @param data additional error information
 * @tparam Node message node type
 */
private[automorph] final case class ResponseError[Node](
  message: String,
  code: Int,
  data: Option[Node]
) {

  def formed: MessageError[Node] = MessageError[Node](
    message = Some(message),
    code = Some(code),
    data = data
  )
}

case object ResponseError {

  private[automorph] def apply[Node](error: MessageError[Node]): ResponseError[Node] = {
    val message = mandatory(error.message, "message")
    val code = mandatory(error.code, "code")
    new ResponseError(message, code, error.data)
  }

  /**
   * Assemble detailed trace of an exception and its causes.
   *
   * @param throwable exception
   * @param maxCauses maximum number of included exception causes
   * @return error messages
   */
  private[automorph] def trace(throwable: Throwable, maxCauses: Int = 100): Seq[String] =
    LazyList.iterate(Option(throwable))(_.flatMap(error => Option(error.getCause)))
      .takeWhile(_.isDefined).flatten.take(maxCauses).map { throwable =>
      val exceptionName = throwable.getClass.getSimpleName
      val message = Option(throwable.getMessage).getOrElse("")
      s"[$exceptionName] $message"
    }
}