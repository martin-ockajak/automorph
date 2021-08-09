package automorph.transport.local.client

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.spi.RpcProtocol.InvalidResponseException
import automorph.spi.transport.ClientMessageTransport
import automorph.spi.{EffectSystem, MessageCodec}
import scala.collection.immutable.ArraySeq

/**
 * Local handler transport passing requests directly to specified ''handler'' using specified ''backend''.
 *
 * @param handler JSON-RPC request handler layer
 * @param system effect system plugin
 * @param defaultContext default request context
 * @constructor Creates a local handler transport passing requests directly to specified ''handler'' using specified ''backend''.
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context request context type
 */
case class HandlerTransport[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  handler: Handler[Node, Codec, Effect, Context],
  system: EffectSystem[Effect],
  defaultContext: Context
) extends ClientMessageTransport[Effect, Context] {

  override def call(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[ArraySeq.ofByte] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    system.flatMap(
      handler.processRequest(request),
      (result: HandlerResult[ArraySeq.ofByte]) =>
        result.response.map(response => system.pure(response)).getOrElse {
          system.failed(InvalidResponseException("Missing call response", None.orNull))
        }
    )
  }

  override def notify(request: ArraySeq.ofByte, mediaType: String, context: Option[Context]): Effect[Unit] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    system.map(
      handler.processRequest(request),
      (_: HandlerResult[ArraySeq.ofByte]) => ()
    )
  }

  override def close(): Effect[Unit] = system.pure(())
}
