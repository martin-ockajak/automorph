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
final case class HandlerBinding[Node, Effect[_], Context](
  function: RpcFunction,
  argumentDecoders: Map[String, Option[Node] => Any],
  encodeResult: Any => (Node, Option[Context]),
  call: (Seq[Any], Context) => Effect[Any],
  invoke: (Seq[Option[Node]], Context) => Effect[(Node, Option[Context])],
  acceptsContext: Boolean
)
