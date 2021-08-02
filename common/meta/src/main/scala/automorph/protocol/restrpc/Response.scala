package automorph.protocol.restrpc

import automorph.protocol.Protocol.fromResponse
import automorph.spi.Message

/**
 * REST-RPC call response.
 *
 * @param result call result
 * @param error call error
 * @tparam Node message node type
 */
private[automorph] final case class Response[Node](
  result: Option[Node],
  error: Option[ResponseError[Node]]
) {

  def formed: Message[Node] = Message[Node](
    jsonrpc = None,
    id = None,
    method = None,
    params = None,
    result = result,
    error = error.map(_.formed)
  )
}

private[automorph] case object Response {

  def apply[Node](message: Message[Node]): Response[Node] = {
    message.result.map { result =>
      Response(Some(result), None)
    }.getOrElse {
      val error = fromResponse(message.error, "error")
      Response(None, Some(ResponseError(error)))
    }
  }
}
