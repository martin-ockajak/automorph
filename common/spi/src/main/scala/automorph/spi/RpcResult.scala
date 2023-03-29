package automorph.spi

import java.io.InputStream

/**
 * RPC handler request processing result.
 *
 * @param responseBody
 *   response message body
 * @param exception
 *   failed call exception
 * @param context
 *   response context
 * @tparam Context
 *   response context type
 */
final case class RpcResult[Context](
  responseBody: InputStream,
  exception: Option[Throwable],
  context: Option[Context],
)
