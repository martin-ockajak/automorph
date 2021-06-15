package jsonrpc.protocol

import ResponseError.mandatory
import jsonrpc.protocol.ResponseError.InvalidRequest
import jsonrpc.spi.Message.{Id, version}
import jsonrpc.spi.{Message, MessageError}

/**
 * JSON-RPC call response.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param id call identifier
 * @param value response value, either a result or an error
 * @tparam Node message node representation type
 */
private[jsonrpc] final case class Response[Node](
  id: Id,
  value: Either[ResponseErrorx[Node], Node]
) {

  def formed: Message[Node] = Message[Node](
    jsonrpc = Some(version),
    id = Some(id),
    method = None,
    params = None,
    result = value.toOption,
    error = value.swap.toOption.map(_.formed)
  )
}

private[jsonrpc] case object Response {

  def apply[Node](message: Message[Node]): Response[Node] = {
    val jsonrpc = mandatory(message.jsonrpc, "jsonrpc")
    if (jsonrpc != version) {
      throw InvalidRequest(s"Invalid JSON-RPC protocol version: $jsonrpc", None.orNull)
    }
    val id = mandatory(message.id, "id")
    message.result.map { result =>
      Response(id, Right(result))
    }.getOrElse {
      val error = mandatory(message.error, "error")
      Response(id, Left(ResponseErrorx(error)))
    }
  }
}

/**
 * JSON-RPC call response error.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param code error code
 * @param message error description
 * @param data additional error information
 * @tparam Node message node representation type
 */
private[jsonrpc] final case class ResponseErrorx[Node](
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

private[jsonrpc] case object ResponseErrorx {

  def apply[Node](error: MessageError[Node]): ResponseErrorx[Node] = {
    val code = mandatory(error.code, "code")
    val message = mandatory(error.message, "message")
    new ResponseErrorx(code, message, error.data)
  }
}
