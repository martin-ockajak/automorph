package automorph.handler

import automorph.Handler
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}

/**
 * RPC request handler builder.
 *
 * @constructor Creates a new RPC request handler builder.
 * @param protocol RPC protocol plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Context message context type
 */
case class ProtocolHandlerBuilder[Node, Codec <: MessageCodec[Node], Context](
  protocol: RpcProtocol[Node, Codec, Context]
) {

  /**
   * Set specified effect system plugin.
   *
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return RPC request handler builder
   */
  def system[Effect[_]](system: EffectSystem[Effect]): Handler[Node, Codec, Effect, Context] =
    Handler(protocol, system)
}
