package automorph.protocol.restrpc

import automorph.protocol.restrpc.Response.mandatory

/**
 * REST-RPC call response error.
 *
 * @param message error description
 * @param code error code
 * @param details additional error information
 * @tparam Node message node type
 */
private[automorph] final case class ResponseError[Node](
  message: String,
  code: Option[Int],
  details: Option[Node]
) {

  def formed: MessageError[Node] = MessageError[Node](
    message = Some(message),
    code = code,
    details = details
  )
}

private[automorph] case object ResponseError {

  def apply[Node](error: MessageError[Node]): ResponseError[Node] = {
    val message = mandatory(error.message, "message")
    val code = mandatory(error.code, "code")
    new ResponseError(message, Some(code), error.details)
  }
}
