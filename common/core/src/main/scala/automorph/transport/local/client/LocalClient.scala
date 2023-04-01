package automorph.transport.local.client

import automorph.RpcException.InvalidResponse
import automorph.spi.{ClientTransport, EffectSystem, RequestHandler}
import automorph.transport.local.LocalContext
import automorph.transport.local.client.LocalClient.Context
import automorph.util.Extensions.EffectOps
import java.io.InputStream

/**
 * Local client transport plugin.
 *
 * Passes RPC API requests directly to the specified RPC request handler.
 *
 * @param effectSystem
 *   effect system plugin
 * @param handler
 *   RPC request handler
 * @param context
 *   default request context
 * @constructor
 *   Creates a local client transport plugin
 * @tparam Effect
 *   effect type
 */
final case class LocalClient[Effect[_]](
  effectSystem: EffectSystem[Effect],
  context: Context = LocalContext.defaultContext,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy[Effect, Context],
) extends ClientTransport[Effect, Context] {

  implicit private val system: EffectSystem[Effect] = effectSystem

  override def call(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
    mediaType: String,
  ): Effect[(InputStream, Context)] =
    handler.processRequest(requestBody, requestContext, requestId).flatMap(_.map { result =>
      effectSystem.successful(result.responseBody -> result.context.getOrElse(context))
    }.getOrElse(effectSystem.failed(InvalidResponse("Missing call response", None.orNull))))

  override def tell(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
    mediaType: String,
  ): Effect[Unit] =
    handler.processRequest(requestBody, requestContext, requestId).map(_ => ())

  override def init(): Effect[Unit] =
    effectSystem.successful(())

  override def close(): Effect[Unit] =
    effectSystem.successful(())
}

object LocalClient {

  /** Message context type. */
  type Context = LocalContext
}
