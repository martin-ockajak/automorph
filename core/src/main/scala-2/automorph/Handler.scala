package automorph

import automorph.handler.{HandlerBind, HandlerBinding, HandlerCore}
import automorph.log.Logging
import automorph.protocol.ErrorType
import automorph.spi.{Backend, Codec}
import automorph.util.{CannotEqual, NoContext}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * JSON-RPC request handler.
 *
 * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
 *
 * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
 * @constructor Creates a new JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request `Context` type.
 * @param codec hierarchical message format codec plugin
 * @param backend effectful computation backend plugin
 * @param exceptionToError maps an exception classs to a corresponding JSON-RPC error type
 * @tparam Node message node type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  codec: ExactCodec,
  backend: Backend[Effect],
  methodBindings: Map[String, HandlerBinding[Node, Effect, Context]],
  protected val exceptionToError: Class[_ <: Throwable] => ErrorType,
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, ExactCodec, Effect, Context]
  with HandlerBind[Node, ExactCodec, Effect, Context]
  with CannotEqual
  with Logging

case object Handler {

  /**
   * Creates a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param codec hierarchical message format codec plugin
   * @param backend effectful computation backend plugin
   * @tparam Node message node type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Node, ExactCodec <: Codec[Node], Effect[_], Context](
    codec: ExactCodec,
    backend: Backend[Effect]
  ): Handler[Node, ExactCodec, Effect, Context] =
    macro applyMacro[Node, ExactCodec, Effect, Context]

  def applyMacro[Node: c.WeakTypeTag, ExactCodec <: Codec[Node]: c.WeakTypeTag, Effect[_], Context: c.WeakTypeTag](
    c: blackbox.Context
  )(
    codec: c.Expr[ExactCodec],
    backend: c.Expr[Backend[Effect]]
  ): c.Expr[Handler[Node, ExactCodec, Effect, Context]] = {
    import c.universe.{weakTypeOf, Quasiquote}
    Seq(weakTypeOf[Node], weakTypeOf[ExactCodec], weakTypeOf[Context])

    c.Expr[Any](q"""
      new automorph.Handler($codec, $backend, Map.empty, automorph.handler.HandlerCore.defaultExceptionToError,
        value => $codec.encode[List[String]](value), $codec.encode(None))
    """).asInstanceOf[c.Expr[Handler[Node, ExactCodec, Effect, Context]]]
  }

  /**
   * Creates a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with empty request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to invoke bound API methods based on incoming JSON-RPC requests.
   *
   * @see [[https://www.automorph.org/specification JSON-RPC protocol specification]]
   * @param codec hierarchical message format codec plugin
   * @param backend effectful computation backend plugin
   * @tparam Node message node type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def noContext[Node, ExactCodec <: Codec[Node], Effect[_]](
    codec: ExactCodec,
    backend: Backend[Effect]
  ): Handler[Node, ExactCodec, Effect, NoContext.Value] =
    macro noContextMacro[Node, ExactCodec, Effect]

  def noContextMacro[Node: c.WeakTypeTag, ExactCodec <: Codec[Node]: c.WeakTypeTag, Effect[_]](c: blackbox.Context)(
    codec: c.Expr[ExactCodec],
    backend: c.Expr[Backend[Effect]]
  ): c.Expr[Handler[Node, ExactCodec, Effect, NoContext.Value]] = {
    import c.universe.{weakTypeOf, Quasiquote}
    Seq(weakTypeOf[Node], weakTypeOf[ExactCodec])

    c.Expr[Any](q"""
      automorph.Handler($codec, $backend, Map.empty, automorph.handler.HandlerCore.defaultExceptionToError,
        value => $codec.encode[List[String]](value), $codec.encode(None))
    """).asInstanceOf[c.Expr[Handler[Node, ExactCodec, Effect, NoContext.Value]]]
  }
}
