package automorph.protocol.jsonrpc

import automorph.protocol.Protocol.{InvalidResponseException, fromResponse}
import automorph.spi.Message
import automorph.spi.Message.{Id, version}

/**
 * JSON-RPC call response.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param id call identifier
 * @param result call result
 * @param error call error
 * @tparam Node message node type
 */
private[automorph] final case class Response[Node](
  id: Id,
  result: Option[Node],
  error: Option[ResponseError[Node]]
) {

  def formed: Message[Node] = Message[Node](
    jsonrpc = Some(version),
    id = Some(id),
    method = None,
    params = None,
    result = result,
    error = error.map(_.formed)
  )
}

private[automorph] case object Response {

  def apply[Node](message: Message[Node]): Response[Node] = {
    val jsonrpc = fromResponse(message.jsonrpc, "automorph")
    if (jsonrpc != version) {
      throw InvalidResponseException(s"Invalid JSON-RPC protocol version: $jsonrpc", None.orNull)
    }
    val id = fromResponse(message.id, "id")
    message.result.map { result =>
      Response(id, Some(result), None)
    }.getOrElse {
      val error = fromResponse(message.error, "error")
      Response(id, None, Some(ResponseError(error)))
    }
  }
}
