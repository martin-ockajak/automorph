package automorph.client

import automorph.util.Method

/**
 * Client bound API method binding.
 *
 * @param method method descriptor
 * @param encodeArguments bound method arguments encoding function
 * @param decodeResult bound method result decoding function
 * @param usesContext true if the last parameter of the bound method is contextual
 * @tparam Node message node type
 */
final case class ClientBinding[Node](
  method: Method,
  encodeArguments: Seq[Any] => Seq[Node],
  decodeResult: Node => Any,
  usesContext: Boolean
)
