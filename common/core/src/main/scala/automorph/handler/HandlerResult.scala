package automorph.handler

/**
 * RPC handler request processing result.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param responseBody response message body
 * @param exception failed call exception
 * @tparam MessageBody message body type
 */
final case class HandlerResult[MessageBody](
  responseBody: Option[MessageBody],
  exception: Option[Throwable]
)
