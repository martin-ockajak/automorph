package jsonrpc.handler

import jsonrpc.spi.Message

/**
 * JSON-RPC handler request processing result.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param response response message
 * @param id call identifier, a request without and identifier is considered to be a notification
 * @param method invoked method name
 * @param errorCode failed call error code
 * @tparam Response response message type
 */
final case class HandlerResult[Response](
  response: Option[Response],
  id: Option[Message.Id],
  method: Option[String],
  errorCode: Option[Int]
)
