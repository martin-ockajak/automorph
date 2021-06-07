package jsonrpc

import java.io.{ByteArrayInputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.Protocol.{MethodNotFound, ParseError}
import jsonrpc.core.{Empty, Protocol, Request, Response, ResponseError}
import jsonrpc.handler.{HandlerMeta, HandlerProcessor, HandlerResult, MethodHandle}
import jsonrpc.log.Logging
import jsonrpc.spi.{Backend, Codec, Message, MessageError}
import jsonrpc.util.CannotEqual
import jsonrpc.util.EncodingOps.toArraySeq
import jsonrpc.util.ValueOps.{asLeft, asRight, asSome, className}
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
  protected val methodBindings: Map[String, MethodHandle[Node, Effect, Context]],
  protected val encodeStrings: Seq[String] => Node
) extends HandlerProcessor[Node, CodecType, Effect, Context]
  with HandlerMeta[Node, CodecType, Effect, Context] with CannotEqual with Logging

object Handler:

  type NoContext = Empty[Handler[?, ?, ?, ?]]
  given NoContext = Empty[Handler[?, ?, ?, ?]]()

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
  inline def basic[Node, CodecType <: Codec[Node], Effect[_]](
    codec: CodecType,
    backend: Backend[Effect],
    bufferSize: Int = 4096
  ): Handler[Node, CodecType, Effect, NoContext] =
    Handler(codec, backend, bufferSize, Map.empty, value => codec.encode[Seq[String]](value))

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
  inline def apply[Node, CodecType <: Codec[Node], Effect[_], Context](
    codec: CodecType,
    backend: Backend[Effect],
    bufferSize: Int = 4096
  ): Handler[Node, CodecType, Effect, Context] =
    new Handler(codec, backend, bufferSize, Map.empty, value => codec.encode[Seq[String]](value))
