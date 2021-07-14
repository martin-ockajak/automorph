package automorph.protocol

import automorph.protocol.ErrorType.InvalidResponseException
import automorph.spi.Message
import automorph.spi.Message.{Id, version}

/**
 * JSON-RPC call response.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param id call identifier
 * @param value response value, either a result or an error
 * @tparam Node message node type
 */
private[automorph] final case class Response[Node](
  id: Id,
  value: Either[ResponseError[Node], Node]
) {

  def formed: Message[Node] = Message[Node](
    automorph = Some(version),
    id = Some(id),
    method = None,
    params = None,
    result = value.toOption,
    error = value.swap.toOption.map(_.formed)
  )
}

private[automorph] case object Response {

  def apply[Node](message: Message[Node]): Response[Node] = {
    val automorph = mandatory(message.automorph, "automorph")
    if (automorph != version) {
      throw InvalidResponseException(s"Invalid JSON-RPC protocol version: $automorph", None.orNull)
    }
    val id = mandatory(message.id, "id")
    message.result.map { result =>
      Response(id, Right(result))
    }.getOrElse {
      val error = mandatory(message.error, "error")
      Response(id, Left(ResponseError(error)))
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
  private[automorph] def mandatory[T](value: Option[T], name: String): T = value.getOrElse(
    throw InvalidResponseException(s"Missing message property: $name", None.orNull)
  )
}
