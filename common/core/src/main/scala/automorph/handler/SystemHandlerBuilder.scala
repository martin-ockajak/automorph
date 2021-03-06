package automorph.handler

import automorph.Handler
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}

/**
 * RPC request handler builder.
 *
 * @constructor Creates a new RPC request handler builder.
 * @param system effect system plugin
 * @tparam Effect effect type
 */
case class SystemHandlerBuilder[Effect[_]](
  system: EffectSystem[Effect]
) {

  /**
   * Set specified RPC protocol plugin.
   *
   * @param protocol RPC protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Context message context type
   * @return RPC request handler builder
   */
  def protocol[Node, Codec <: MessageCodec[Node], Context](
    protocol: RpcProtocol[Node, Codec, Context]
  ): Handler[Node, Codec, Effect, Context] =
    Handler(protocol, system)
}
