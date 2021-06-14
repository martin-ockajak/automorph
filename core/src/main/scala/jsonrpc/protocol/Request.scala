package jsonrpc.protocol

import jsonrpc.protocol.Errors.{mandatory, InvalidRequest}
import jsonrpc.spi.Message
import jsonrpc.spi.Message.{version, Id, Params}

/**
 * JSON-RPC request.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param id call identifier, a request without and identifier is considered to be a notification
 * @param method invoked method name
 * @param params invoked method argument values by position or by name
 * @tparam Node message node representation type
 */
final case class Request[Node](
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

case object Request {

  def apply[Node](message: Message[Node]): Request[Node] = {
    val jsonrpc = mandatory(message.jsonrpc, "jsonrpc")
    if (jsonrpc != version) {
      throw InvalidRequest(s"Invalid JSON-RPC protocol version: $jsonrpc", None.orNull)
    }
    val id = message.id
    val method = mandatory(message.method, "method")
    val params = message.params.getOrElse(Right(Map.empty[String, Node]))
    Request(id, method, params)
  }
}
