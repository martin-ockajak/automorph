package automorph.handler

import automorph.spi.protocol.RpcFunction

/**
 * RPC handler remote API function binding.
 *
 * @param function bound function descriptor
 * @param invoke invokes bound function
 * @param acceptsContext true if the method accepts request context as its last parameter, false otherwise
 * @tparam Node message node type
 * @tparam Effect effect type
 * @tparam Context message context type
 */
final private[automorph] case class HandlerBinding[Node, Effect[_], Context](
  function: RpcFunction,
  invoke: (Seq[Option[Node]], Context) => Effect[(Node, Option[Context])],
  acceptsContext: Boolean
)
