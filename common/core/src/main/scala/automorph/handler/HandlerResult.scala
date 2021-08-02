package automorph.handler

import automorph.spi.Message

/**
 * JSON-RPC handler request processing result.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param response response message
 * @param errorCode failed call error code
 * @tparam Data message data type
 */
final case class HandlerResult[Data](
  response: Option[Data],
  errorCode: Option[Int]
)
