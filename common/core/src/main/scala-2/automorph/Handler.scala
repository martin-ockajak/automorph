package automorph

import automorph.handler.{HandlerBind, HandlerBinding, HandlerCore}
import automorph.log.Logging
import automorph.spi.{EffectSystem, MessageFormat, RpcProtocol}
import automorph.util.{CannotEqual, EmptyContext}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * RPC request handler.
 *
 * The handler can be used by a RPC server to invoke bound API methods based on incoming RPC requests.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a new RPC request handler with specified request `Context` type plus specified ''format'' and ''system'' plugins.
 * @param format message format plugin
 * @param system effect system plugin
 * @param protocol RPC protocol
 * @param encodeStrings converts list of strings to message format node
 * @param encodedNone message format node representing missing optional value
 * @tparam Node message node type
 * @tparam Format message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, Format <: MessageFormat[Node], Effect[_], Context](
  format: Format,
  system: EffectSystem[Effect],
  protocol: RpcProtocol,
  methodBindings: Map[String, HandlerBinding[Node, Effect, Context]],
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, Format, Effect, Context]
  with HandlerBind[Node, Format, Effect, Context]
  with CannotEqual
  with Logging

case object Handler {

  /** Handler with arbitrary format. */
  type AnyFormat[Effect[_], Context] = Handler[Node, _ <: MessageFormat[Node], Effect, Context] forSome { type Node }

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
  def apply[Node, Format <: MessageFormat[Node], Effect[_], Context](
    format: Format,
    system: EffectSystem[Effect]
  ): Handler[Node, Format, Effect, Context] =
    macro applyMacro[Node, Format, Effect, Context]

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
  def withoutContext[Node, Format <: MessageFormat[Node], Effect[_]](
    format: Format,
    system: EffectSystem[Effect]
  ): Handler[Node, Format, Effect, EmptyContext.Value] =
    macro withoutContextMacro[Node, Format, Effect]

  def applyMacro[Node: c.WeakTypeTag, Format <: MessageFormat[Node]: c.WeakTypeTag, Effect[_], Context: c.WeakTypeTag](
    c: blackbox.Context
  )(
    format: c.Expr[Format],
    system: c.Expr[EffectSystem[Effect]]
  ): c.Expr[Handler[Node, Format, Effect, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Format], weakTypeOf[Context])

    c.Expr[Any](q"""
      new automorph.Handler($format,
        $system,
        automorph.protocol.jsonrpc.JsonRpcProtocol(),
        Map.empty,
        value => $format.encode[List[String]](value), $format.encode(None)
      )
    """).asInstanceOf[c.Expr[Handler[Node, Format, Effect, Context]]]
  }

  def withoutContextMacro[Node: c.WeakTypeTag, Format <: MessageFormat[Node]: c.WeakTypeTag, Effect[_]](c: blackbox.Context)(
    format: c.Expr[Format],
    system: c.Expr[EffectSystem[Effect]]
  ): c.Expr[Handler[Node, Format, Effect, EmptyContext.Value]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Format])

    c.Expr[Any](q"""
      automorph.Handler(
        $format,
        $system,
        automorph.protocol.jsonrpc.JsonRpcProtocol(),
        Map.empty,
        value => $format.encode[List[String]](value), $format.encode(None)
      )
    """).asInstanceOf[c.Expr[Handler[Node, Format, Effect, EmptyContext.Value]]]
  }
}
