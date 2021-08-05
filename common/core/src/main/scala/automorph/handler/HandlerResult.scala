package automorph.handler

/**
 * RPC handler request processing result.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param response response message
 * @param exception failed call exception
 * @tparam Data message data type
 */
final case class HandlerResult[Data](
  response: Option[Data],
  exception: Option[Throwable]
)
