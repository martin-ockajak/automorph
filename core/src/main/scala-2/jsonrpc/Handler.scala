package jsonrpc

import jsonrpc.handler.{HandlerMeta, HandlerMethod, HandlerCore}
import jsonrpc.log.Logging
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.util.{CannotEqual, NoContext}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * JSON-RPC request handler layer.
 *
 * The handler can be used by a JSON-RPC server to process incoming JSON-RPC requests, invoke the requested API methods and return JSON-RPC responses.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request `Context` type.
 * @param codec message codec plugin
 * @param backend effect backend plugin
 * @param bufferSize input stream reading buffer size
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  codec: ExactCodec,
  backend: Backend[Effect],
  bufferSize: Int,
  methodBindings: Map[String, HandlerMethod[Node, Effect, Context]],
  protected val encodeStrings: List[String] => Node,
  protected val encodedNone: Node
) extends HandlerCore[Node, ExactCodec, Effect, Context]
  with HandlerMeta[Node, ExactCodec, Effect, Context]
  with CannotEqual
  with Logging

case object Handler {
  /** Default handler buffer size. */
  val defaultBufferSize = 4096

  /**
   * Create a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request Context type.
   *
   * The handler can be used by a JSON-RPC server to process incoming requests, invoke the requested API methods and generate outgoing responses.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node message format node representation type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Node, ExactCodec <: Codec[Node], Effect[_], Context](
    codec: ExactCodec,
    backend: Backend[Effect],
    bufferSize: Int
  ): Handler[Node, ExactCodec, Effect, Context] =
    macro applyMacro[Node, ExactCodec, Effect, Context]

  def applyMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag
  ](c: blackbox.Context)(
    codec: c.Expr[ExactCodec],
    backend: c.Expr[Backend[Effect]],
    bufferSize: c.Expr[Int]
  ): c.Expr[Handler[Node, ExactCodec, Effect, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[ExactCodec], weakTypeOf[Context])

    c.Expr[Any]( q"""
      jsonrpc.Handler($codec, $backend, $bufferSize, Map.empty, value => $codec.encode[List[String]](value), $codec.encode(None))
    """).asInstanceOf[c.Expr[Handler[Node, ExactCodec, Effect, Context]]]
  }

  /**
   * Create a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins with defined request Context type.
   *
   * The handler can be used by a JSON-RPC server to process incoming requests, invoke the requested API methods and generate outgoing responses.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @tparam Node message format node representation type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Node, ExactCodec <: Codec[Node], Effect[_], Context](
    codec: ExactCodec,
    backend: Backend[Effect]
  ): Handler[Node, ExactCodec, Effect, Context] =
    macro applyDefaultMacro[Node, ExactCodec, Effect, Context]

  def applyDefaultMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag
  ](c: blackbox.Context)(
    codec: c.Expr[ExactCodec],
    backend: c.Expr[Backend[Effect]]
  ): c.Expr[Handler[Node, ExactCodec, Effect, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[ExactCodec], weakTypeOf[Context])

    c.Expr[Any](q"""
      jsonrpc.Handler($codec, $backend, Handler.defaultBufferSize, Map.empty, value => $codec.encode[List[String]](value), $codec.encode(None))
    """).asInstanceOf[c.Expr[Handler[Node, ExactCodec, Effect, Context]]]
  }

  /**
   * Create a JSON-RPC request handler using the specified ''codec'' and ''backend'' plugins without request `Context` type.
   *
   * The handler can be used by a JSON-RPC server to process incoming requests, invoke the requested API methods and generate outgoing responses.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec hierarchical message codec plugin
   * @param backend effect backend plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node message format node representation type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def basic[Node, ExactCodec <: Codec[Node], Effect[_]](
    codec: ExactCodec,
    backend: Backend[Effect]
  ): Handler[Node, ExactCodec, Effect, NoContext.Value] =
    macro basicMacro[Node, ExactCodec, Effect]

  def basicMacro[
    Node: c.WeakTypeTag,
    ExactCodec <: Codec[Node]: c.WeakTypeTag,
    Effect[_]
  ](c: blackbox.Context)(
    codec: c.Expr[ExactCodec],
    backend: c.Expr[Backend[Effect]]
  ): c.Expr[Handler[Node, ExactCodec, Effect, NoContext.Value]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[ExactCodec])

    c.Expr[Any](q"""
      jsonrpc.Handler($codec, $backend, Handler.defaultBufferSize, Map.empty, value => $codec.encode[List[String]](value), $codec.encode(None))
    """).asInstanceOf[c.Expr[Handler[Node, ExactCodec, Effect, NoContext.Value]]]
  }
}
