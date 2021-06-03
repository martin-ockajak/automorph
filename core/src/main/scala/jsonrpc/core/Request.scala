package jsonrpc.core

import jsonrpc.core.Protocol.{InvalidRequestException, mandatory}
import jsonrpc.spi.Message
import jsonrpc.spi.Message.{Id, Params, version}
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
  id: Option[Id],
  method: String,
  params: Params[Node]
):
  def formed: Message[Node] = Message[Node](
    jsonrpc = version.asSome,
    id = id,
    method = method.asSome,
    params = params.asSome,
    result = None,
    error = None
  )

case object Request:
  def apply[Node](message: Message[Node]): Request[Node] =
    val jsonrpc = mandatory(message.jsonrpc, "jsonrpc")
    if jsonrpc != version then
      throw InvalidRequestException(s"Invalid JSON-RPC protocol version: $jsonrpc", None.orNull)
    val id = message.id
    val method = mandatory(message.method, "method")
    val params = message.params.getOrElse(Map.empty.asRight[List[Node]])
    Request(id, method, params)
