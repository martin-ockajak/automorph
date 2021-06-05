package jsonrpc.transport.local

import jsonrpc.JsonRpcHandler
import jsonrpc.spi.{Codec, Backend, Transport}
import scala.collection.immutable.ArraySeq

/**
 * Local handler transport.
 *
 * @param handler JSON-RPC request handler layer
 * @param backend effect backend plugin
 * @tparam Node message format node representation type
 * @tparam CodecType message format codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, CodecType <: Codec[Node], Effect[_], Context](
  handler: JsonRpcHandler[Node, CodecType, Effect, Context],
  backend: Backend[Effect]
) extends Transport[Effect, Context]:

  def call(request: ArraySeq.ofByte, context: Context): Effect[ArraySeq.ofByte] =
    backend.map(handler.processRequest(request)(using context), { result =>
      result.response.getOrElse(throw IllegalStateException("Missing call response"))
    })

  def notify(request: ArraySeq.ofByte, context: Context): Effect[Unit] =
    backend.map(handler.processRequest(request)(using context), _ => ())
