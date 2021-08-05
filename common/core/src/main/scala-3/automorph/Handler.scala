package automorph

import automorph.handler.{HandlerBind, HandlerBinding, HandlerCore}
import automorph.log.Logging
import automorph.protocol.Protocol
import automorph.protocol.jsonrpc.JsonRpcProtocol
import automorph.spi.{EffectSystem, MessageFormat}
import automorph.util.{CannotEqual, EmptyContext}

/**
 * Automorph RPC request handler.
 *
 * Used by an RPC server to invoke bound API methods based on incoming RPC requests.
 *
 * @constructor Creates a new RPC request handler with specified request `Context` type plus specified ''format'' and ''system'' plugins.
 * @param format message format plugin
 * @param system effect system plugin
 * @param protocol RPC protocol
 * @param encodedStrings converts list of strings to message format node
 * @param encodedNone message format node representing missing optional value
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, Format <: MessageFormat[Node], Effect[_], Context](
  format: Format,
  system: EffectSystem[Effect],
  protocol: Protocol,
  methodBindings: Map[String, HandlerBinding[Node, Effect, Context]],
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, Format, Effect, Context]
  with HandlerBind[Node, Format, Effect, Context]
  with CannotEqual
  with Logging

case object Handler:

  /** Handler with arbitrary format. */
  type AnyFormat[Effect[_], Context] = Handler[_, _, Effect, Context]

  /**
   * Creates a RPC request handler with specified request `Context` type plus specified ''format'' and ''system'' plugins.
   *
   * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
   *
   * @param format message format plugin
   * @param system effect system plugin
   * @tparam Node message node type
   * @tparam Format message format plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  inline def apply[Node, Format <: MessageFormat[Node], Effect[_], Context](
    format: Format,
    system: EffectSystem[Effect]
  ): Handler[Node, Format, Effect, Context] =
    val encodeStrings = (value: List[String]) => format.encode[List[String]](value)
    Handler(format, system, JsonRpcProtocol(), Map.empty, encodeStrings, format.encode(None))

  /**
   * Creates a RPC request handler with empty request context plus specified specified ''format'' and ''system'' plugins.
   *
   * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
   *
   * @param format message format plugin
   * @param system effect system plugin
   * @tparam Node message node type
   * @tparam Format message format plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return RPC request handler
   */
  inline def withoutContext[Node, Format <: MessageFormat[Node], Effect[_]](
    format: Format,
    system: EffectSystem[Effect]
  ): Handler[Node, Format, Effect, EmptyContext.Value] =
    val encodeStrings = (value: List[String]) => format.encode[List[String]](value)
    Handler(format, system, JsonRpcProtocol(), Map.empty, encodeStrings, format.encode(None))
