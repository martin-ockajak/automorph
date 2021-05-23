package jsonrpc.core

import jsonrpc.core.Protocol.{ErrorType, InvalidRequestException}
import jsonrpc.spi.Message
import jsonrpc.util.ValueOps.{asRight, asSome}

/**
 * JSON-RPC request.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param jsonrpc protocol version (must be 2.0)
 * @param id call identifier, a request without and identifier is considered to be a notification
 * @param method invoked method name
 * @param params invoked method argument values by position or by name
 * @tparam Node message node representation type
 */
final case class Request[Node](
  id: Option[Protocol.Id],
  method: String,
  params: Either[List[Node], Map[String, Node]]
):
  def message: Message[Node] = Message[Node](
    jsonrpc = Protocol.version.asSome,
    id = id,
    method = method.asSome,
    params = params.asSome,
    result = None,
    error = None
  )

  lazy val details: Map[String, String] = Map(
    "Type" -> id.map(_ => RequestType.Call).getOrElse(RequestType.Notification).toString,
    "Method" -> method,
    "Arguments" -> params.fold(_.size, _.size).toString
  ) ++ id.map(value => "Id" -> value.fold(_.toString, identity))

case object Request:
  def apply[Node](message: Message[Node]): Request[Node] =
    val jsonrpc = Protocol.mandatory(message.jsonrpc, "jsonrpc")
    if jsonrpc != Protocol.version then
      throw InvalidRequestException(s"Invalid JSON-RPC protocol version: $jsonrpc", None.orNull)
    val id = message.id
    val method = Protocol.mandatory(message.method, "method")
    val params = message.params.getOrElse(Map.empty.asRight[List[Node]])
    Request(id, method, params)

/** JSON-RPC request types. */
enum RequestType:
  case Call, Notification
