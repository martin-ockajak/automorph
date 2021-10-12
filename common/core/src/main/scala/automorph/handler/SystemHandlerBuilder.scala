package automorph.handler

import automorph.Handler
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.EmptyContext

/**
 * RPC request ''handler'' builder.
 *
 * @constructor Creates a new RPC request ''handler'' builder.
 * @param system effect ''system'' plugin
 * @tparam Effect effect type
 */
case class SystemHandlerBuilder[Effect[_]](
  system: EffectSystem[Effect]
) {

  /**
   * Set specified RPC ''protocol'' plugin.
   *
   * @param protocol RPC ''protocol'' plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return RPC request ''handler'' builder
   */
  def protocol[Node, Codec <: MessageCodec[Node]](
    protocol: RpcProtocol[Node, Codec]
  ): FullHandlerBuilder[Node, Codec, Effect] = FullHandlerBuilder(protocol, system)
}
