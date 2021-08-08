package automorph.spi.protocol

/**
 * RPC error.
 *
 * @constructor Creates RPC error.
 * @param exception exception causing the error
 * @param message RPC message
 * @tparam Content protocol-specific message content type
 */
final case class RpcError[Content](
  exception: Throwable,
  message: RpcMessage[Content]
)
