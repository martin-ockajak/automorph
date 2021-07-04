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
 * @tparam Data message data type
 */
final case class HandlerResult[Data](
  response: Option[Data],
  id: Option[Message.Id],
  method: Option[String],
  errorCode: Option[Int]
)
