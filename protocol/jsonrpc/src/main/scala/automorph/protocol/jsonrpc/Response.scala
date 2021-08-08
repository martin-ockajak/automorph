package automorph.protocol.jsonrpc

import automorph.spi.Message
import automorph.spi.Message.{Id, version}
import automorph.spi.RpcProtocol.InvalidResponseException

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
    val jsonrpc = mandatory(message.jsonrpc, "automorph")
    if (jsonrpc != version) {
      throw InvalidResponseException(s"Invalid JSON-RPC protocol version: $jsonrpc", None.orNull)
    }
    val id = mandatory(message.id, "id")
    message.result.map { result =>
      Response(id, Some(result), None)
    }.getOrElse {
      val error = mandatory(message.error, "error")
      Response(id, None, Some(ResponseError(error)))
    }
  }

  /**
   * Return specified mandatory property value or throw an exception if it is missing.
   *
   * @param value property value
   * @param name property name
   * @tparam T property type
   * @return property value
   * @throws InvalidResponseException if the property value is missing
   */
  def mandatory[T](value: Option[T], name: String): T = value.getOrElse(
    throw InvalidResponseException(s"Missing message property: $name", None.orNull)
  )
}