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
 * @tparam ResponseMessage response message type
 */
final case class HandlerResult[ResponseMessage](
  response: Option[ResponseMessage],
  id: Option[Message.Id],
  method: Option[String],
  errorCode: Option[Int]
)
