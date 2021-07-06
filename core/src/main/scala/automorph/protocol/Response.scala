package automorph.protocol

import automorph.protocol.ErrorType.{InvalidRequestException, mandatory}
import automorph.spi.Message.{Id, version}
import automorph.spi.Message

/**
 * JSON-RPC call response.
 *
 * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
 * @param id call identifier
 * @param value response value, either a result or an error
 * @tparam Node message node representation type
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
      throw InvalidRequestException(s"Invalid JSON-RPC protocol version: $automorph", None.orNull)
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
