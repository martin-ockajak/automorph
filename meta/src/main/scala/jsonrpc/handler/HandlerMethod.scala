package jsonrpc.handler

/**
 * Handler bound API method binding.
 *
 * @param invoke bound method invocation function
 * @param name method name
 * @param resultType result type
 * @param paramNames parameter names
 * @param parameterTypes paramter types
 * @tparam Node message format node representation type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class HandlerMethod[Node, Effect[_], Context](
  invoke: (Seq[Node], Context) => Effect[Node],
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String],
  usesContext: Boolean
)
