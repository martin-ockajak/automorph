package automorph

import automorph.handler.{HandlerBinding, HandlerCore, HandlerMeta}
import automorph.log.Logging
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}
import scala.collection.immutable.ListMap

/**
 * Automorph RPC request handler.
 *
 * Used by an RPC server to invoke bound API methods based on incoming RPC requests.
 *
 * @constructor Creates a new RPC request handler with specified ''system'' and ''protocol'' plugins providing corresponding request context type.
 * @param system effect system plugin
 * @param protocol RPC protocol
 * @param bindings API method bindings
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  system: EffectSystem[Effect],
  protocol: RpcProtocol[Node, Codec],
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
   * Creates a RPC request handler with specified ''system'' and ''protocol'' plugins without providing request context.
   *
   * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
   *
   * @param system effect system plugin
   * @param protocol RPC protocol
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  def withoutContext[Node, Codec <: MessageCodec[Node], Effect[_]](
    system: EffectSystem[Effect],
    protocol: RpcProtocol[Node, Codec]
  ): Handler[Node, Codec, Effect, EmptyContext.Value] =
    Handler(system, protocol, ListMap.empty)
}
