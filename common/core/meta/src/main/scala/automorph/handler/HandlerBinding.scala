package automorph.handler

import automorph.spi.protocol.RpcFunction

/**
 * RPC handler remote API function binding.
 *
 * @param function RPC function
 * @param invoke invokes bound function
 * @param usesContext true if the last parameter of the bound function is contextual
 * @tparam Node message node type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class HandlerBinding[Node, Effect[_], Context](
  function: RpcFunction,
  invoke: (Seq[Node], Context) => Effect[Node],
  usesContext: Boolean
)
