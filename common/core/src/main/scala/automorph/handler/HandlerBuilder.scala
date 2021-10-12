package automorph.handler

import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.EmptyContext
import automorph.Handler

/**
 * RPC request handler builder.
 *
 * @constructor Creates a new RPC request handler builder.
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
case class HandlerBuilder[Node, Codec <: MessageCodec[Node], Effect[_], Context] (
  private val system: Option[EffectSystem[Effect]] = None,
  private val protocol: Option[RpcProtocol[Node, Codec]] = None,
) {
  /**
   * Set specified effect ''system'' plugin.
   *
   * @param system effect ''system'' plugin
   * @tparam Effect effect type
   * @return RPC request handler builder
   */
  def system[Effect[_]](system: EffectSystem[Effect]): HandlerBuilder[Node, Codec, Effect, Context] =
    copy(system = Some(system))

  /**
   * Set specified RPC ''protocol'' plugin.
   *
   * @param protocol RPC ''protocol'' plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return RPC request handler builder
   */
  def protocol[Node, Codec <: MessageCodec[Node]](protocol: RpcProtocol[Node, Codec]): HandlerBuilder[Node, Codec, Effect, Context] =
    copy(protocol = Some(protocol))

  /**
   * Set specified request context type.
   *
   * @tparam Context request context type
   * @return RPC request handler builder
   */
  def context[Context]: HandlerBuilder[Node, Codec, Effect, Context] = copy()

  /**
   * Build an RPC request handler.
   *
   * @return RPC request handler
   */
  def build: Handler[Node, Codec, Effect, Context] =
    Handler(get(protocol, "RPC protocol"), get(system, "effect system"))

  private def get[T](plugin: Option[T], name: String): T =
    plugin.getOrElse(throw new IllegalStateException(s"Missing $name plugin"))
}

object HandlerBuilder {
  /**
   * Creates a new RPC request handler builder.
   *
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @return RPC request handler builder
   */
  def apply[Node, Codec <: MessageCodec[Node], Effect[_]](): HandlerBuilder[Node, Codec, Effect, EmptyContext.Value] =
    new HandlerBuilder()
}
