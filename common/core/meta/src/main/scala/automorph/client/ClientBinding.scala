package automorph.client

import automorph.spi.protocol.RpcFunction

/**
 * RPC client remote API function binding.
 *
 * @param function bound function descriptor
 * @param argumentEncoders map of method parameter names to argument encoding functions
 * @param decodeResult decodes bound function result
 * @param acceptsContext true if the last parameter of the bound function is contextual
 * @tparam Node message node type
 * @tparam Context message context type
 */
final private[automorph] case class ClientBinding[Node, Context](
  function: RpcFunction,
  argumentEncoders: Map[String, Any => Node],
  decodeResult: (Node, Context) => Any,
  acceptsContext: Boolean,
)
