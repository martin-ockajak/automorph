package jsonrpc.client

import jsonrpc.JsonRpcClient
import jsonrpc.core.Empty
import jsonrpc.spi.{Backend, Codec, Transport}

trait ClientFactory:

  type NoContext = Empty[JsonRpcClient[?, ?, ?, ?]]
  given NoContext = Empty[JsonRpcClient[?, ?, ?, ?]]()

  /**
   * Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins with defined request `Context` type.
   *
   * The client can be used by an application to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node message format node representation type
   * @tparam CodecType message codec plugin type
   * @tparam Effect effect type
   * @tparam Context request context type
   * @return JSON-RPC request client
   */
  inline def apply[Node, CodecType <: Codec[Node], Effect[_], Context](
    codec: CodecType,
    backend: Backend[Effect],
    transport: Transport[Effect, Context]
  ): JsonRpcClient[Node, CodecType, Effect, Context]

  /**
   * Create a JSON-RPC client using the specified ''codec'', ''backend'' and ''transport'' plugins without request `Context` type.
   *
   * The client can be used by an application to perform JSON-RPC calls and notifications.
   *
   * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
   * @param codec message codec plugin
   * @param backend effect backend plugin
   * @param bufferSize input stream reading buffer size
   * @tparam Node message format node representation type
   * @tparam CodecType message codec plugin type
   * @tparam Effect effect type
   * @return JSON-RPC request client
   */
  inline def basic[Node, CodecType <: Codec[Node], Effect[_]](
    codec: CodecType,
    backend: Backend[Effect],
    transport: Transport[Effect, NoContext]
  ): JsonRpcClient[Node, CodecType, Effect, NoContext]
