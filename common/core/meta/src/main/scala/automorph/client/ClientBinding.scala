package automorph.client

import automorph.spi.protocol.RpcFunction

/**
 * RPC client remote API function binding.
 *
 * @param function method descriptor
 * @param encodeArguments bound method arguments encoding function
 * @param decodeResult bound method result decoding function
 * @param usesContext true if the last parameter of the bound method is contextual
 * @tparam Node message node type
 */
final case class ClientBinding[Node](
  function: RpcFunction,
  encodeArguments: Seq[Any] => Seq[Node],
  decodeResult: Node => Any,
  usesContext: Boolean
)
