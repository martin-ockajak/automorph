package jsonrpc.transport.local

import jsonrpc.JsonRpcHandler
import jsonrpc.spi.{Codec, Effect, Transport}
import scala.collection.immutable.ArraySeq

/**
 * Local handler transport.
 *
 * @param handler request handler layer
 * @param effect effect system plugin
 * @tparam Node data format node representation type
 * @tparam CodecType data format codec plugin type
 * @tparam Outcome effectful computation outcome type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, CodecType <: Codec[Node], Outcome[_], Context](
  handler: JsonRpcHandler[Node, CodecType, Outcome, Context],
  effect: Effect[Outcome]
) extends Transport[Outcome, Context]:

  def call(request: ArraySeq.ofByte, context: Context): Outcome[ArraySeq.ofByte] =
    effect.map(handler.processRequest(request)(using context), { response =>
      response.getOrElse(throw IllegalStateException("Missing call response"))
    })

  def notify(request: ArraySeq.ofByte, context: Context): Outcome[Unit] =
    effect.map(handler.processRequest(request)(using context), _ => ())
