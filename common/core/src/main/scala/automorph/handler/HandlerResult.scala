package automorph.handler

/**
 * RPC handler request processing result.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param responseBody response message body
 * @param exception failed call exception
 * @param context response context
 * @tparam MessageBody message body type
 * @tparam Context response context type
 */
final case class HandlerResult[MessageBody, Context](
  responseBody: Option[MessageBody],
  exception: Option[Throwable],
  context: Option[Context]
)
