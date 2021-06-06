package jsonrpc

import jsonrpc.client.ClientFactory
import jsonrpc.spi.{Backend, Codec, Transport}

case object JsonRpcClientFactory extends ClientFactory:

  inline def apply[Node, CodecType <: Codec[Node], Effect[_], Context](
    codec: CodecType,
    backend: Backend[Effect],
    transport: Transport[Effect, Context]
  ): JsonRpcClient[Node, CodecType, Effect, Context] =
    new JsonRpcClient(codec, backend, transport)

  inline def basic[Node, CodecType <: Codec[Node], Effect[_]](
    codec: CodecType,
    backend: Backend[Effect],
    transport: Transport[Effect, NoContext]
  ): JsonRpcClient[Node, CodecType, Effect, NoContext] =
    new JsonRpcClient(codec, backend, transport)
