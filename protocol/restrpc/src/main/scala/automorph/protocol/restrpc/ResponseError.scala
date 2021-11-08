package automorph.protocol.restrpc

import automorph.protocol.restrpc.Response.mandatory

/**
 * REST-RPC call response error.
 *
 * @param message error message
 * @param code error code
 */
private[automorph] final case class ResponseError(
  message: String,
  code: Option[Int]
) {

  def formed: MessageError = MessageError(
    message = Some(message),
    code = code
  )
}

private[automorph] object ResponseError {

  def apply(error: MessageError): ResponseError = {
    val message = mandatory(error.message, "message")
    val code = mandatory(error.code, "code")
    new ResponseError(message, Some(code))
  }
}
