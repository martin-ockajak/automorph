package automorph.handler

/**
 * RPC handler request processing result.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param response response message
 * @param exception failed call exception
 * @tparam MessageBody message body type
 */
final case class HandlerResult[MessageBody](
  response: Option[MessageBody],
  exception: Option[Throwable]
)
