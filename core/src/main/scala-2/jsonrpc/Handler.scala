package jsonrpc

import jsonrpc.handler.{HandlerMeta, HandlerMethod, HandlerProcessor}
import jsonrpc.log.Logging
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.util.{CannotEqual, Void}
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
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
final case class Handler[Node, CodecType <: Codec[Node], Effect[_], Context](
  codec: CodecType,
  backend: Backend[Effect],
  bufferSize: Int,
  protected val encodeStrings: Seq[String] => Node,
  protected val encodedNone: Node,
  protected val methodBindings: Map[String, HandlerMethod[Node, Effect, Context]]
) extends HandlerProcessor[Node, CodecType, Effect, Context]
  with HandlerMeta[Node, CodecType, Effect, Context]
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
   * @tparam CodecType message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Node, CodecType <: Codec[Node], Effect[_], Context](
    codec: CodecType,
    backend: Backend[Effect],
    bufferSize: Int
  ): Handler[Node, CodecType, Effect, Context] =
    macro applyMacro[Node, CodecType, Effect, Context]

  def applyMacro[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag
  ](c: blackbox.Context)(
    codec: c.Expr[CodecType],
    backend: c.Expr[Backend[Effect]],
    bufferSize: c.Expr[Int]
  ): c.Expr[Handler[Node, CodecType, Effect, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[CodecType], weakTypeOf[Context])

    c.Expr[Handler[Node, CodecType, Effect, Context]]( q"""
      jsonrpc.Handler($codec, $backend, $bufferSize, value => $codec.encode[Seq[String]](value), $codec.encode(None), Map.empty)
    """)
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
   * @tparam CodecType message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def apply[Node, CodecType <: Codec[Node], Effect[_], Context](
    codec: CodecType,
    backend: Backend[Effect]
  ): Handler[Node, CodecType, Effect, Context] =
    macro applyDefaultMacro[Node, CodecType, Effect, Context]

  def applyDefaultMacro[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_],
    Context: c.WeakTypeTag
  ](c: blackbox.Context)(
    codec: c.Expr[CodecType],
    backend: c.Expr[Backend[Effect]]
  ): c.Expr[Handler[Node, CodecType, Effect, Context]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[CodecType], weakTypeOf[Context])

    c.Expr[Handler[Node, CodecType, Effect, Context]](q"""
      jsonrpc.Handler($codec, $backend, Handler.defaultBufferSize, value => $codec.encode[Seq[String]](value), $codec.encode(None), Map.empty)
    """)
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
   * @tparam CodecType message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request handler
   */
  def basic[Node, CodecType <: Codec[Node], Effect[_]](
    codec: CodecType,
    backend: Backend[Effect]
  ): Handler[Node, CodecType, Effect, Void.Value] =
    macro basicMacro[Node, CodecType, Effect]

  def basicMacro[
    Node: c.WeakTypeTag,
    CodecType <: Codec[Node]: c.WeakTypeTag,
    Effect[_]
  ](c: blackbox.Context)(
    codec: c.Expr[CodecType],
    backend: c.Expr[Backend[Effect]]
  ): c.Expr[Handler[Node, CodecType, Effect, Void.Value]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[CodecType])

    c.Expr[Handler[Node, CodecType, Effect, Void.Value]](q"""
      jsonrpc.Handler($codec, $backend, Handler.defaultBufferSize, value => $codec.encode[Seq[String]](value), $codec.encode(None), Map.empty)
    """)
  }
}
