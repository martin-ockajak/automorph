package automorph.transport.local

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.protocol.ErrorType.InvalidResponseException
import automorph.spi.{Backend, Codec, ClientTransport}
import scala.collection.immutable.ArraySeq

/**
 * Local handler transport passing requests directly to specified ''handler'' using specified ''backend''.
 *
 * @param handler JSON-RPC request handler layer
 * @param backend effect system plugin
 * @param defaultContext default request context
 * @constructor Creates a local handler transport passing requests directly to specified ''handler'' using specified ''backend''.
 * @tparam Node message node type
 * @tparam ActualCodec message format codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, ActualCodec <: Codec[Node], Effect[_], Context](
  handler: Handler[Node, ActualCodec, Effect, Context],
  backend: Backend[Effect],
  defaultContext: Context
) extends ClientTransport[Effect, Context] {

  def call(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[ArraySeq.ofByte] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    backend.flatMap(
      handler.processRequest(request),
      (result: HandlerResult[ArraySeq.ofByte]) =>
        result.response.map(backend.pure).getOrElse(backend.failed(InvalidResponseException("Missing call response", None.orNull)))
    )
  }

  def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    backend.map(
      handler.processRequest(request),
      (_: HandlerResult[ArraySeq.ofByte]) => ()
    )
  }
}
