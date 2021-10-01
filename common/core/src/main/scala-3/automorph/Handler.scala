package automorph

import automorph.handler.{HandlerBinding, HandlerCore, HandlerMeta}
import automorph.log.Logging
import automorph.spi.{EffectSystem, MessageCodec, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}

/**
 * Automorph RPC request handler.
 *
 * Used by an RPC server to invoke bound API methods based on incoming RPC requests.
 *
 * @constructor Creates a new RPC request handler with specified request `Context` type plus specified ''codec'' and ''system'' plugins.
 * @param codec message codec plugin
 * @param system effect system plugin
 * @param protocol RPC protocol
 * @param bindings API method bindings
 * @param encodedNone message codec node representing missing optional value
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, Codec <: MessageCodec[Node], Effect[_], Context] (
  codec: Codec,
  system: EffectSystem[Effect],
  protocol: RpcProtocol[Node],
  bindings: Map[String, HandlerBinding[Node, Effect, Context]],
  protected val encodedNone: Node
) extends HandlerCore[Node, Codec, Effect, Context]
  with HandlerMeta[Node, Codec, Effect, Context]
  with CannotEqual
  with Logging

object Handler:

  /** Handler with arbitrary codec. */
  type AnyCodec[Effect[_], Context] = Handler[_, _, Effect, Context]

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
  inline def apply[Node, Codec <: MessageCodec[Node], Effect[_], Context](
    codec: Codec,
    system: EffectSystem[Effect],
    protocol: RpcProtocol[Node]
  ): Handler[Node, Codec, Effect, Context] =
    Handler(codec, system, protocol, Map.empty, codec.encode(None))

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
  inline def withoutContext[Node, Codec <: MessageCodec[Node], Effect[_]](
    codec: Codec,
    system: EffectSystem[Effect],
    protocol: RpcProtocol[Node]
  ): Handler[Node, Codec, Effect, EmptyContext.Value] =
    Handler(codec, system, protocol, Map.empty, codec.encode(None))
