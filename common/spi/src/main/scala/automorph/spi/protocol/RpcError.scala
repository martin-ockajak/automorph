package automorph.spi.protocol

/**
 * RPC error.
 *
 * @constructor Creates RPC error.
 * @param exception exception causing the error
 * @param message RPC message
 * @tparam Details protocol-specific message details type
 */
final case class RpcError[Details](
  exception: Throwable,
  message: RpcMessage[Details]
)
