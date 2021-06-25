package jsonrpc.protocol

import jsonrpc.protocol.ErrorType.{InvalidRequestException, mandatory}
import jsonrpc.spi.Message.{Id, version}
import jsonrpc.spi.Message

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
  value: Either[ResponseError[Node], Node]
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
      throw InvalidRequestException(s"Invalid JSON-RPC protocol version: $jsonrpc", None.orNull)
    }
    val id = mandatory(message.id, "id")
    message.result.map { result =>
      Response(id, Right(result))
    }.getOrElse {
      val error = mandatory(message.error, "error")
      Response(id, Left(ResponseError(error)))
    }
  }
}
