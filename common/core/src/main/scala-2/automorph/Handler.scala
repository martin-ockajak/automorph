package automorph

import automorph.handler.{HandlerBind, HandlerBinding, HandlerCore}
import automorph.log.Logging
import automorph.protocol.ErrorType
import automorph.spi.{EffectSystem, MessageFormat}
import automorph.util.{CannotEqual, EmptyContext}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * JSON-RPC request handler.
 *
 * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a new JSON-RPC request handler with specified request `Context` type plus specified ''format'' and ''system'' plugins.
 * @param format message format plugin
 * @param system effect system plugin
 * @param exceptionToError maps an exception classs to a corresponding JSON-RPC error type
 * @tparam Node message node type
 * @tparam ActualFormat message format plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, ActualFormat <: MessageFormat[Node], Effect[_], Context](
  format: ActualFormat,
  system: EffectSystem[Effect],
  methodBindings: Map[String, HandlerBinding[Node, Effect, Context]],
  protected val exceptionToError: Class[_ <: Throwable] => ErrorType,
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, ActualFormat, Effect, Context]
  with HandlerBind[Node, ActualFormat, Effect, Context]
  with CannotEqual
  with Logging

case object Handler {

  /** Handler with arbitrary format. */
  type AnyFormat[Effect[_], Context] = Handler[Node, _ <: MessageFormat[Node], Effect, Context] forSome { type Node }

  /**
   * Creates a JSON-RPC request handler with specified request `Context` type plus specified ''format'' and ''system'' plugins.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param format message format plugin
   * @param system effect system plugin
   * @tparam Node message node type
   * @tparam ActualFormat message format plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Node, ActualFormat <: MessageFormat[Node], Effect[_], Context](
    format: ActualFormat,
    system: EffectSystem[Effect]
  ): Handler[Node, ActualFormat, Effect, Context] =
    macro applyMacro[Node, ActualFormat, Effect, Context]

  /**
   * Creates a JSON-RPC request handler with empty request context plus specified specified ''format'' and ''system'' plugins.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param format message format plugin
   * @param system effect system plugin
   * @tparam Node message node type
   * @tparam ActualFormat message format plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def withoutContext[Node, ActualFormat <: MessageFormat[Node], Effect[_]](
    format: ActualFormat,
    system: EffectSystem[Effect]
  ): Handler[Node, ActualFormat, Effect, EmptyContext.Value] =
    macro withoutContextMacro[Node, ActualFormat, Effect]

  def applyMacro[Node: c.WeakTypeTag, ActualFormat <: MessageFormat[Node]: c.WeakTypeTag, Effect[_], Context: c.WeakTypeTag](
    c: blackbox.Context
  )(
    format: c.Expr[ActualFormat],
    system: c.Expr[EffectSystem[Effect]]
  ): c.Expr[Handler[Node, ActualFormat, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}
    Seq(weakTypeOf[Node], weakTypeOf[ActualFormat], weakTypeOf[Context])

    c.Expr[Any](q"""
      new automorph.Handler($format, $system, Map.empty, automorph.handler.HandlerCore.defaultExceptionToError,
        value => $format.encode[List[String]](value), $format.encode(None))
    """).asInstanceOf[c.Expr[Handler[Node, ActualFormat, Effect, Context]]]
  }

  def withoutContextMacro[Node: c.WeakTypeTag, ActualFormat <: MessageFormat[Node]: c.WeakTypeTag, Effect[_]](c: blackbox.Context)(
    format: c.Expr[ActualFormat],
    system: c.Expr[EffectSystem[Effect]]
  ): c.Expr[Handler[Node, ActualFormat, Effect, EmptyContext.Value]] = {
    import c.universe.{weakTypeOf, Quasiquote}
    Seq(weakTypeOf[Node], weakTypeOf[ActualFormat])

    c.Expr[Any](q"""
      automorph.Handler($format, $system, Map.empty, automorph.handler.HandlerCore.defaultExceptionToError,
        value => $format.encode[List[String]](value), $format.encode(None))
    """).asInstanceOf[c.Expr[Handler[Node, ActualFormat, Effect, EmptyContext.Value]]]
  }
}
