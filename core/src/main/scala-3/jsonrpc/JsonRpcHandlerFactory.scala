package jsonrpc

import jsonrpc.handler.HandlerFactory
import jsonrpc.spi.{Backend, Codec}

case object JsonRpcHandlerFactory extends HandlerFactory:

  inline def basic[Node, CodecType <: Codec[Node], Effect[_]](
    codec: CodecType,
    backend: Backend[Effect],
    bufferSize: Int = 4096
  ): JsonRpcHandler[Node, CodecType, Effect, NoContext] =
    new JsonRpcHandler(codec, backend, bufferSize, Map.empty, value => codec.encode[Seq[String]](value))

  inline def apply[Node, CodecType <: Codec[Node], Effect[_], Context](
    codec: CodecType,
    backend: Backend[Effect],
    bufferSize: Int = 4096
  ): JsonRpcHandler[Node, CodecType, Effect, Context] =
    new JsonRpcHandler(codec, backend, bufferSize, Map.empty, value => codec.encode[Seq[String]](value))
