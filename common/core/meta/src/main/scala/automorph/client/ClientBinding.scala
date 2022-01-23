package automorph.client

import automorph.spi.protocol.RpcFunction

/**
 * RPC client remote API function binding.
 *
 * @param function bound function descriptor
 * @param encodeArguments encodes bound function arguments
 * @param decodeResult decodes bound function result
 * @param acceptsContext true if the last parameter of the bound function is contextual
 * @tparam Node message node type
 * @tparam Context message context type
 */
final private[automorph] case class ClientBinding[Node, Context](
  function: RpcFunction,
  encodeArguments: Seq[Any] => Seq[Node],
  decodeResult: (Node, Context) => Any,
  acceptsContext: Boolean
)
