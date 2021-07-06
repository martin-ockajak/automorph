package automorph.protocol

import automorph.protocol.ErrorType.{InvalidRequestException, mandatory}
import automorph.spi.Message
import automorph.spi.Message.{Id, Params, version}

/**
 * JSON-RPC request.
 *
 * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
 * @param id call identifier, a request without and identifier is considered to be a notification
 * @param method invoked method name
 * @param params invoked method argument values by position or by name
 * @tparam Node message node representation type
 */
private[automorph] final case class Request[Node](
  id: Option[Id],
  method: String,
  params: Params[Node]
) {

  def formed: Message[Node] = Message[Node](
    automorph = Some(version),
    id = id,
    method = Some(method),
    params = Some(params),
    result = None,
    error = None
  )
}

private[automorph] case object Request {

  def apply[Node](message: Message[Node]): Request[Node] = {
    val automorph = mandatory(message.automorph, "automorph")
    if (automorph != version) {
      throw InvalidRequestException(s"Invalid JSON-RPC protocol version: $automorph", None.orNull)
    }
    val id = message.id
    val method = mandatory(message.method, "method")
    val params = message.params.getOrElse(Right(Map.empty[String, Node]))
    Request(id, method, params)
  }
}
