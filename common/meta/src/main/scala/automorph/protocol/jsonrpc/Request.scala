package automorph.protocol.jsonrpc

import automorph.protocol.jsonrpc.ErrorType.InvalidRequestException
import automorph.spi.Message
import automorph.spi.Message.{Id, Params, version}

/**
 * JSON-RPC request.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param id call identifier, a request without and identifier is considered to be a notification
 * @param method invoked method name
 * @param params invoked method argument values by position or by name
 * @tparam Node message node type
 */
private[automorph] final case class Request[Node](
  id: Option[Id],
  method: String,
  params: Params[Node]
) {

  def formed: Message[Node] = Message[Node](
    jsonrpc = Some(version),
    id = id,
    method = Some(method),
    params = Some(params),
    result = None,
    error = None
  )
}

private[automorph] case object Request {

  def apply[Node](message: Message[Node]): Request[Node] = {
    val automorph = mandatory(message.jsonrpc, "automorph")
    if (automorph != version) {
      throw InvalidRequestException(s"Invalid JSON-RPC protocol version: $automorph", None.orNull)
    }
    val id = message.id
    val method = mandatory(message.method, "method")
    val params = message.params.getOrElse(Right(Map.empty[String, Node]))
    Request(id, method, params)
  }

  /**
   * Return specified mandatory property value or throw an exception if it is missing.
   *
   * @param value property value
   * @param name property name
   * @tparam T property type
   * @return property value
   * @throws InvalidRequestException if the property value is missing
   */
  private[automorph] def mandatory[T](value: Option[T], name: String): T = value.getOrElse(
    throw InvalidRequestException(s"Missing message property: $name", None.orNull)
  )
}
