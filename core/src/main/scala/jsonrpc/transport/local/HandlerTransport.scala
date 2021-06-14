package jsonrpc.transport.local

import jsonrpc.Handler
import jsonrpc.spi.{Backend, Codec, Transport}
import scala.collection.immutable.ArraySeq

/**
 * Local handler transport.
 *
 * @param handler JSON-RPC request handler layer
 * @param backend effect backend plugin
 * @param defaultContext default request context
 * @tparam Node message format node representation type
 * @tparam CodecType message format codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, CodecType <: Codec[Node], Effect[_], Context](
  handler: Handler[Node, CodecType, Effect, Context],
  backend: Backend[Effect],
  defaultContext: Context
) extends Transport[Effect, Context] {

  def call(request: ArraySeq.ofByte, context: Option[Context]): Effect[ArraySeq.ofByte] =
    backend.map(
      handler.processRequest(request)(context.getOrElse(defaultContext)),
      result => result.response.getOrElse(throw IllegalStateException("Missing call response"))
    )

  def notify(request: ArraySeq.ofByte, context: Option[Context]): Effect[Unit] =
    backend.map(handler.processRequest(request)(context.getOrElse(defaultContext)), _ => ())
}
