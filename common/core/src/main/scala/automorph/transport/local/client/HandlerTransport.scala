package automorph.transport.local.client

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.spi.Protocol.InvalidResponseException
import automorph.spi.{ClientMessageTransport, EffectSystem, MessageFormat}
import scala.collection.immutable.ArraySeq

/**
 * Local handler transport passing requests directly to specified ''handler'' using specified ''backend''.
 *
 * @param handler JSON-RPC request handler layer
 * @param backend effect system plugin
 * @param defaultContext default request context
 * @constructor Creates a local handler transport passing requests directly to specified ''handler'' using specified ''backend''.
 * @tparam Node message node type
 * @tparam Format message format codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, Format <: MessageFormat[Node], Effect[_], Context](
  handler: Handler[Node, Format, Effect, Context],
  backend: EffectSystem[Effect],
  defaultContext: Context
) extends ClientMessageTransport[Effect, Context] {

  override def call(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[ArraySeq.ofByte] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    backend.flatMap(
      handler.processRequest(request),
      (result: HandlerResult[ArraySeq.ofByte]) =>
        result.response.map(backend.pure).getOrElse(backend.failed(InvalidResponseException("Missing call response", None.orNull)))
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    backend.map(
      handler.processRequest(request),
      (_: HandlerResult[ArraySeq.ofByte]) => ()
    )
  }

  override def close(): Unit = ()
}
