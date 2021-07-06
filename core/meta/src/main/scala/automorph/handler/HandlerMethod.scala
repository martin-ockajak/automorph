package automorph.handler

import automorph.util.Method

/**
 * Handler bound API method binding.
 *
 * @param method method descriptor
 * @param invoke bound method invocation function
 * @param name method name
 * @param resultType result type
 * @param paramNames parameter names
 * @param parameterTypes paramter types
 * @param usesContext true if the last parameter of the bound method is contextual
 * @tparam Node message format node representation type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class HandlerMethod[Node, Effect[_], Context](
//  method: Method,
  invoke: (Seq[Node], Context) => Effect[Node],
  name: String,
  resultType: String,
  paramNames: Seq[String],
  parameterTypes: Seq[String],
  usesContext: Boolean
)
