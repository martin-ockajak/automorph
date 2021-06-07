package jsonrpc.handler

/**
 * Bound API method binding.
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
final case class HandlerMethod[Node, Effect[_], Context](
  function: (Seq[Node], Context) => Effect[Node],
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String],
  usesContext: Boolean
)
