package automorph.handler

import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}

/**
 * RPC request handler builder.
 *
 * @constructor Creates a new RPC request handler builder.
 * @param protocol RPC protocol plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
case class ProtocolHandlerBuilder[Node, Codec <: MessageCodec[Node]](
  protocol: RpcProtocol[Node, Codec]
) {

  /**
   * Set specified effect system plugin.
   *
   * @param system effect system plugin
   * @tparam Effect effect type
   * @return RPC request handler builder
   */
  def system[Effect[_]](system: EffectSystem[Effect]): FullHandlerBuilder[Node, Codec, Effect] =
    FullHandlerBuilder(protocol, system)
}
