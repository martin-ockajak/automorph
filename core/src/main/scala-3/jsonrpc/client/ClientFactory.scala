package jsonrpc.client

import jsonrpc.Client
import jsonrpc.core.Empty
import jsonrpc.spi.{Backend, Codec, Transport}

case object ClientFactory:

  type NoContext = Empty[Client[?, ?, ?, ?]]
  given NoContext = Empty[Client[?, ?, ?, ?]]()

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
  ): Client[Node, CodecType, Effect, Context] =
    Client(codec, backend, transport)

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
  ): Client[Node, CodecType, Effect, NoContext] =
    Client(codec, backend, transport)
