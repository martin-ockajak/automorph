package automorph.handler

import automorph.Handler
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.EmptyContext

/**
 * RPC request ''handler'' builder.
 *
 * @constructor Creates a new RPC request ''handler'' builder.
 * @param protocol RPC ''protocol'' plugin
 * @param system effect ''system'' plugin
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 */
case class FullHandlerBuilder[Node, Codec <: MessageCodec[Node], Effect[_]](
  protocol: RpcProtocol[Node, Codec],
  system: EffectSystem[Effect]
) {

  /**
   * Creates an RPC request ''handler'' with specified request context type.
   *
   * @tparam Context request context type
   * @return RPC request ''handler''
   */
  def context[Context]: Handler[Node, Codec, Effect, Context] =
    Handler(protocol, system)

  /**
   * Creates an RPC request ''handler'' with empty request context type.
   *
   * @return RPC request ''handler''
   */
  def emptyContext: Handler[Node, Codec, Effect, EmptyContext.Value] =
    Handler(protocol, system)
}
