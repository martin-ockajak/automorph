package jsonrpc.client

/**
 * Client bound API method binding.
 *
 * @param function binding function wrapping the bound method
 * @param name method name
 * @param resultType result type
 * @param paramNames parameter names
 * @param parameterTypes paramter types
 * @tparam Node message format node representation type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class ClientMethod[Node, Effect[_], Context](
  function: (Seq[Any], Context) => Effect[Any],
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String],
  usesContext: Boolean
)
