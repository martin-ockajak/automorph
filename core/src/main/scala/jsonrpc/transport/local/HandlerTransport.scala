package jsonrpc.transport.local

import jsonrpc.Handler
import jsonrpc.handler.HandlerResult
import jsonrpc.protocol.ErrorType.InvalidResponseException
import jsonrpc.spi.{Backend, Codec, Transport}
import scala.collection.immutable.ArraySeq

/**
 * Local handler transport.
 *
 * @param handler JSON-RPC request handler layer
 * @param backend effect backend plugin
 * @param defaultContext default request context
 * @tparam Node message format node representation type
 * @tparam ExactCodec message format codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, ExactCodec <: Codec[Node], Effect[_], Context](
  handler: Handler[Node, ExactCodec, Effect, Context],
  backend: Backend[Effect],
  defaultContext: Context
) extends Transport[Effect, Context] {

  def call(request: ArraySeq.ofByte, context: Option[Context]): Effect[ArraySeq.ofByte] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    backend.flatMap(
      handler.processRequest(request),
      (result: HandlerResult[ArraySeq.ofByte]) =>
        result.response.map(backend.pure).getOrElse(backend.failed(InvalidResponseException("Missing call response", None.orNull)))
    )
  }

  def notify(request: ArraySeq.ofByte, context: Option[Context]): Effect[Unit] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    backend.map(
      handler.processRequest(request),
      (_: HandlerResult[ArraySeq.ofByte]) => ()
    )
  }
}
