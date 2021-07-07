package automorph.client

import automorph.util.Method

/**
 * Client bound API method binding.
 *
 * @param method method descriptor
 * @param encodeArguments bound method arguments encoding function
 * @param decodeResult bound method result decoding function
 * @param name method name
 * @param resultType result type
 * @param paramNames parameter names
 * @param parameterTypes paramter types
 * @param usesContext true if the last parameter of the bound method is contextual
 * @tparam Node message format node representation type
 */
final case class ClientBinding[Node](
//  method: Method,
  encodeArguments: Seq[Any] => Seq[Node],
  decodeResult: Node => Any,
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String],
  usesContext: Boolean
)
