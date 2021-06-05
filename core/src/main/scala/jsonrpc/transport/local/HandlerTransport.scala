package jsonrpc.transport.local

import jsonrpc.JsonRpcHandler
import jsonrpc.spi.{Codec, Backend, Transport}
import scala.collection.immutable.ArraySeq

/**
 * Local handler transport.
 *
 * @param handler JSON-RPC request handler layer
 * @param effect effect system plugin
 * @tparam Node message format node representation type
 * @tparam CodecType message format codec plugin type
 * @tparam Outcome effectful computation outcome type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, CodecType <: Codec[Node], Outcome[_], Context](
  handler: JsonRpcHandler[Node, CodecType, Outcome, Context],
  effect: Backend[Outcome]
) extends Transport[Outcome, Context]:

  def call(request: ArraySeq.ofByte, context: Context): Outcome[ArraySeq.ofByte] =
    effect.map(handler.processRequest(request)(using context), { result =>
      result.response.getOrElse(throw IllegalStateException("Missing call response"))
    })

  def notify(request: ArraySeq.ofByte, context: Context): Outcome[Unit] =
    effect.map(handler.processRequest(request)(using context), _ => ())
