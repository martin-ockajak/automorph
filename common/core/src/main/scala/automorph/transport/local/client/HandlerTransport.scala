package automorph.transport.local.client

import automorph.Handler
import automorph.RpcException.InvalidResponseException
import automorph.spi.{ClientTransport, EffectSystem, MessageCodec}
import automorph.util.Extensions.EffectOps
import java.io.InputStream

/**
 * Local handler transport passing requests directly to specified ''handler'' using specified ''backend''.
 *
 * @param handler JSON-RPC request handler layer
 * @param effectSystem effect system plugin
 * @param defaultContext default request context
 * @constructor Creates a local handler transport passing requests directly to specified ''handler'' using specified ''backend''.
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Effect effect type
 * @tparam Context message context type
 */
case class HandlerTransport[Node, Codec <: MessageCodec[Node], Effect[_], Context](
  handler: Handler[Node, Codec, Effect, Context],
  effectSystem: EffectSystem[Effect],
  defaultContext: Context
) extends ClientTransport[Effect, Context] {

  implicit private val givenSystem: EffectSystem[Effect] = effectSystem

  override def call(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
    mediaType: String
  ): Effect[(InputStream, Context)] =
    handler.processRequest(requestBody, requestContext, requestId)
      .flatMap { result => result.responseBody.map { responseBody =>
          effectSystem.successful(responseBody -> result.context.getOrElse(defaultContext))
        }.getOrElse {
          effectSystem.failed(InvalidResponseException("Missing call response", None.orNull))
        }
      }

  override def message(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
    mediaType: String
  ): Effect[Unit] =
    handler.processRequest(requestBody, requestContext, requestId).map(_ => ())

  override def close(): Effect[Unit] =
    effectSystem.successful(())
}
