package automorph.handler

import scala.collection.immutable.ArraySeq

/**
 * RPC handler request processing result.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param responseBody response message body
 * @param exception failed call exception
 * @param context response context
 * @tparam Context response context type
 */
final case class HandlerResult[Context](
  responseBody: Option[ArraySeq.ofByte],
  exception: Option[Throwable],
  context: Option[Context]
)
