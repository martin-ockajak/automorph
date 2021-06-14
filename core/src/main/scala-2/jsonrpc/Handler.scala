package jsonrpc

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.protocol.Errors.{MethodNotFound, ParseError}
import jsonrpc.protocol.{Errors, Request, Response, ResponseError}
import jsonrpc.handler.{HandlerMeta, HandlerMethod, HandlerProcessor, HandlerResult}
import jsonrpc.log.Logging
import jsonrpc.spi.{Backend, Codec, Message, MessageError}
import jsonrpc.util.{CannotEqual, NoContextFor}
import scala.collection.immutable.ArraySeq
import scala.util.Try

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
  protected val methodBindings: Map[String, HandlerMethod[Node, Effect, Context]],
  protected val encodeStrings: Seq[String] => Node,
  protected val encodedNone: Node
) extends HandlerProcessor[Node, CodecType, Effect, Context]
  with HandlerMeta[Node, CodecType, Effect, Context]
  with CannotEqual
  with Logging

object Handler {

  type NoContext = NoContextFor[Handler[?, ?, ?, ?]]

  implicit val noContext: NoContext = NoContextFor[Handler[?, ?, ?, ?]]()

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
    backend: Backend[Effect],
    bufferSize: Int = 4096
  ): Handler[Node, CodecType, Effect, NoContext] =
    new Handler(codec, backend, bufferSize, Map.empty, value => codec.encode[Seq[String]](value), codec.encode(None))

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
    bufferSize: Int = 4096
  ): Handler[Node, CodecType, Effect, Context] =
    new Handler(codec, backend, bufferSize, Map.empty, value => codec.encode[Seq[String]](value), codec.encode(None))
}
