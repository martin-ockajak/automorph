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
 * @tparam Context message context type
 */
case class HandlerTransport[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  handler: Handler[Node, Codec, Effect, Context],
  system: EffectSystem[Effect],
  defaultContext: Context
) extends ClientMessageTransport[Effect, Context] {

  override def call(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[(ArraySeq.ofByte, Context)] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    system.flatMap(
      handler.processRequest(requestBody, requestId, None),
      (result: HandlerResult[ArraySeq.ofByte, Context]) =>
        result.responseBody.map(response => system.pure(response -> defaultContext)).getOrElse {
          system.failed(InvalidResponseException("Missing call response", None.orNull))
        }
    )
  }

  override def notify(
    requestBody: ArraySeq.ofByte,
    requestId: String,
    mediaType: String,
    context: Option[Context]
  ): Effect[Unit] = {
    implicit val usingContext = context.getOrElse(defaultContext)
    system.map(
      handler.processRequest(requestBody, requestId, None),
      (_: HandlerResult[ArraySeq.ofByte, Context]) => ()
    )
  }

  override def close(): Effect[Unit] =
    system.pure(())
}
