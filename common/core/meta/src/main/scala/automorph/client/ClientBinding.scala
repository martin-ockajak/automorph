package automorph.client

import automorph.spi.protocol.RpcFunction

/**
 * RPC client remote API function binding.
 *
 * @param function function descriptor
 * @param encodeArguments encodes bound function arguments
 * @param decodeResult decodes bound function result
 * @param usesContext true if the last parameter of the bound function is contextual
 * @tparam Node message node type
 */
final case class ClientBinding[Node](
  function: RpcFunction,
  encodeArguments: Seq[Any] => Seq[Node],
  decodeResult: Node => Any,
  usesContext: Boolean
)
