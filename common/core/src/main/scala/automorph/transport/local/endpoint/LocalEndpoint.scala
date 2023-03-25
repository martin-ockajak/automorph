package automorph.transport.local.endpoint

import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}

/**
 * Local endpoint transport plugin.
 *
 * Processes requests directly from a client.
 *
 * @constructor
 *   Creates a local endpoint transport plugin
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
final case class LocalEndpoint[Effect[_], Context](
  effectSystem: EffectSystem[Effect],
  context: Context,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy,
) extends EndpointTransport[Effect, Context, Unit] {

  override def adapter: Unit =
    ()

  override def clone(handler: RequestHandler[Effect, Context]): LocalEndpoint[Effect, Context] =
    copy(handler = handler)
}
