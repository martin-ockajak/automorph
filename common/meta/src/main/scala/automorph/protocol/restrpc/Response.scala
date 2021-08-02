package automorph.protocol.restrpc

import automorph.protocol.jsonrpc.ErrorType.InvalidResponseException
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
      val error = mandatory(message.error, "error")
      Response(None, Some(ResponseError(error)))
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
