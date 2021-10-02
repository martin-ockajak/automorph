package automorph

import automorph.handler.{HandlerBinding, HandlerCore, HandlerMeta}
import automorph.log.Logging
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}
import scala.collection.immutable.ListMap
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * RPC request handler.
 *
 * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a new RPC request handler with specified request `Context` type plus specified ''codec'' and ''system'' plugins.
 * @param codec message codec plugin
 * @param system effect system plugin
 * @param protocol RPC protocol
 * @param encodedNone message codec node representing missing optional value
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  codec: Codec,
  system: EffectSystem[Effect],
  protocol: RpcProtocol[Node],
  bindings: ListMap[String, HandlerBinding[Node, Effect, Context]],
  protected val encodedNone: Node
) extends HandlerCore[Node, Codec, Effect, Context]
  with HandlerMeta[Node, Codec, Effect, Context]
  with CannotEqual
  with Logging

object Handler {

  /** Handler with arbitrary codec. */
  type AnyCodec[Effect[_], Context] = Handler[Node, _ <: MessageCodec[Node], Effect, Context] forSome { type Node }

  /**
   * Creates a RPC request handler with specified request `Context` type plus specified ''codec'' and ''system'' plugins.
   *
   * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
   *
   * @param codec message codec plugin
   * @param system effect system plugin
   * @param protocol RPC protocol
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  def apply[Node, Codec <: MessageCodec[Node], Effect[_], Context](
    codec: Codec,
    system: EffectSystem[Effect],
    protocol: RpcProtocol[Node]
  ): Handler[Node, Codec, Effect, Context] =
    macro applyMacro[Node, Codec, Effect, Context]

  /**
   * Creates a RPC request handler with empty request context plus specified specified ''codec'' and ''system'' plugins.
   *
   * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
   *
   * @param codec message codec plugin
   * @param system effect system plugin
   * @param protocol RPC protocol
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  def withoutContext[Node, Codec <: MessageCodec[Node], Effect[_]](
    codec: Codec,
    system: EffectSystem[Effect],
    protocol: RpcProtocol[Node]
  ): Handler[Node, Codec, Effect, EmptyContext.Value] =
    macro withoutContextMacro[Node, Codec, Effect]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]: c.WeakTypeTag, Effect[_], Context: c.WeakTypeTag](
    c: blackbox.Context
  )(
    codec: c.Expr[Codec],
    system: c.Expr[EffectSystem[Effect]],
    protocol: c.Expr[RpcProtocol[Node]]
  ): c.Expr[Handler[Node, Codec, Effect, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec], weakTypeOf[Context])

    c.Expr[Any](q"""
      new automorph.Handler(
        $codec,
        $system,
        $protocol,
        ListMap.empty,
        $codec.encode(None)
      )
    """).asInstanceOf[c.Expr[Handler[Node, Codec, Effect, Context]]]
  }

  def withoutContextMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]: c.WeakTypeTag, Effect[_]](c: blackbox.Context)(
    codec: c.Expr[Codec],
    system: c.Expr[EffectSystem[Effect]],
    protocol: c.Expr[RpcProtocol[Node]]
  ): c.Expr[Handler[Node, Codec, Effect, EmptyContext.Value]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[Any](q"""
      automorph.Handler(
        $codec,
        $system,
        $protocol,
        ListMap.empty,
        $codec.encode(None)
      )
    """).asInstanceOf[c.Expr[Handler[Node, Codec, Effect, EmptyContext.Value]]]
  }
}
