package automorph.transport.local.server

import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}

/**
 * Local server transport plugin.
 *
 * Processes requests directly from a client.
 *
 * @constructor
 *   Creates a local server transport plugin
 * @param effectSystem
 *   effect system plugin
 * @param context
 *   default request context
 * @param handler
 *   RPC request handler
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   message context type
 */
final case class LocalServer[Effect[_], Context](
  effectSystem: EffectSystem[Effect],
  context: Context,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy,
) extends ServerTransport[Effect, Context] {

  override def clone(rpcHandler: RequestHandler[Effect, Context]): ServerTransport[Effect, Context] =
    copy(handler = handler)

  override def init(): Effect[Unit] =
    effectSystem.successful(())

  override def close(): Effect[Unit] =
    effectSystem.successful(())
}
