package jsonrpc.handler

import jsonrpc.JsonRpcHandler
import jsonrpc.core.Empty
import jsonrpc.spi.{Backend, Codec}

trait HandlerFactory:

  type NoContext = Empty[JsonRpcHandler[?, ?, ?, ?]]
  given NoContext = Empty[JsonRpcHandler[?, ?, ?, ?]]()

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
  ): JsonRpcHandler[Node, CodecType, Effect, NoContext]

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
  ): JsonRpcHandler[Node, CodecType, Effect, Context]
