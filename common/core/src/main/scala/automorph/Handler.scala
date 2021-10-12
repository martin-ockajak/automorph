package automorph

import automorph.handler.{HandlerBinding, HandlerBuilder, HandlerCore, HandlerMeta}
import automorph.log.Logging
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}
import scala.collection.immutable.ListMap

/**
 * RPC request handler.
 *
 * Used by RPC servers to invoke bound API methods based on incoming requests.
 *
 * @constructor Creates a new RPC request handler with specified ''system'' and ''protocol'' plugins providing corresponding request context type.
 * @param protocol RPC ''protocol'' plugin
 * @param system effect ''system'' plugin
 * @param bindings API method bindings
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  protocol: RpcProtocol[Node, Codec],
  system: EffectSystem[Effect],
  protected val bindings: ListMap[String, HandlerBinding[Node, Effect, Context]] =
    ListMap.empty[String, HandlerBinding[Node, Effect, Context]]
) extends HandlerCore[Node, Codec, Effect, Context]
  with HandlerMeta[Node, Codec, Effect, Context]
  with CannotEqual
  with Logging

object Handler {

  /** Handler with arbitrary node type. */
  type AnyCodec[Effect[_], Context] = Handler[_, _, Effect, Context]

  /**
   * Creates an RPC request handler builder.
   *
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @return RPC request handler builder
   */
  def builder[Node, Codec <: MessageCodec[Node], Effect[_]]: HandlerBuilder[Node, Codec, Effect, EmptyContext.Value] =
    HandlerBuilder()
}
