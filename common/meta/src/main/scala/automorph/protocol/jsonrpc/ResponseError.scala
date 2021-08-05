package automorph.protocol.jsonrpc

import automorph.protocol.Protocol.responseMandatory
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

private[automorph] case object ResponseError {

  private[automorph] def apply[Node](error: MessageError[Node]): ResponseError[Node] = {
    val message = responseMandatory(error.message, "message")
    val code = responseMandatory(error.code, "code")
    new ResponseError(message, code, error.data)
  }
}
