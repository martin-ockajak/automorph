package jsonrpc.client

/**
 * Client bound API method binding.
 *
 * @param encodeArguments bound method arguments encoding function
 * @param decodeResult bound method result decoding function
 * @param name method name
 * @param resultType result type
 * @param paramNames parameter names
 * @param parameterTypes paramter types
 * @tparam Node message format node representation type
 * @tparam Context request context type
 */
final case class ClientMethod[Node, Context](
  encodeArguments: (Seq[Any], Context) => Seq[Node],
  decodeResult: Node => Any,
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String],
  usesContext: Boolean
)
