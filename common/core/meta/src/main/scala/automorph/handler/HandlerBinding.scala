package automorph.handler

import automorph.spi.protocol.RpcFunction

/**
 * RPC handler API method binding.
 *
 * @param function RPC function
 * @param invoke bound method invocation function
 * @param usesContext true if the last parameter of the bound method is contextual
 * @tparam Node message node type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class HandlerBinding[Node, Effect[_], Context](
  function: RpcFunction,
  invoke: (Seq[Node], Context) => Effect[Node],
  usesContext: Boolean
)
