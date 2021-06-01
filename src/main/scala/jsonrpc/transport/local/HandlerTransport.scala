package jsonrpc.transport.local

import jsonrpc.JsonRpcHandler
import jsonrpc.spi.{Codec, Effect, Transport}
import scala.collection.immutable.ArraySeq

/**
 * Message transport
 *
 * @param handler
 * @param effect
 * @tparam Node
 * @tparam CodecType
 * @tparam Outcome effectful computation outcome type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, CodecType <: Codec[Node], Outcome[_], Context](
  handler: JsonRpcHandler[Node, CodecType, Outcome, Context],
  effect: Effect[Outcome]
) extends Transport[Outcome, Context]:

  def call(request: ArraySeq.ofByte, context: Option[Context]): Outcome[ArraySeq.ofByte] =
    val contextValue = context.getOrElse(throw IllegalStateException("Missing request context"))
    effect.map(handler.processRequest(request, contextValue), { response =>
      response.getOrElse(throw IllegalStateException("Missing call response"))
    })

  def notify(request: ArraySeq.ofByte, context: Option[Context]): Outcome[Unit] =
    val contextValue = context.getOrElse(throw IllegalStateException("Missing request context"))
    effect.map(handler.processRequest(request, contextValue), _ => ())
