package automorph.spi.protocol

/**
 * RPC error.
 *
 * @constructor
 *   Creates RPC error.
 * @param exception
 *   exception causing the error
 * @param message
 *   RPC message
 * @tparam Metadata
 *   protocol-specific message metadata type
 */
final case class RpcError[Metadata](exception: Throwable, message: RpcMessage[Metadata])
