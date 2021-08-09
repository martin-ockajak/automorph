package automorph.protocol.restrpc

import automorph.protocol.restrpc.Response.mandatory
import automorph.spi.MessageError


/**
 * REST-RPC call response error.
 *
 * @param message error description
 * @param code error code
 * @param data additional error incodecion
 * @tparam Node message node type
 */
private[automorph] final case class ResponseError[Node](
  message: String,
  code: Option[Int],
  data: Option[Node]
) {

  def formed: MessageError[Node] = MessageError[Node](
    message = Some(message),
    code = code,
    data = data
  )
}

private[automorph] case object ResponseError {

  def apply[Node](error: MessageError[Node]): ResponseError[Node] = {
    val message = mandatory(error.message, "message")
    val code = mandatory(error.code, "code")
    new ResponseError(message, Some(code), error.data)
  }
}
