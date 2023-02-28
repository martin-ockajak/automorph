package automorph

import automorph.spi.ServerMessageTransport

/**
 * Active RPC server.
 *
 * The server can be used to serve remote API requests using specific message transport protocol and invoke bound API
 * methods to process them.
 *
 * Remote APIs can be invoked statically using transparent proxy instances automatically derived from specified API
 * traits or dynamically by supplying the required type information on invocation. Undertow HTTP & WebSocket server
 * message transport plugin.
 *
 * The server interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as HTTP response body.
 *
 * Processes only HTTP requests starting with specified URL path.
 *
 * @constructor
 *   Creates active RPC server with specified handler and transport plugin and supporting corresponding message
 *   context type.
 * @param handler
 *   RPC request handler
 * @param transport
 *   server message transport plugin
 * @tparam Effect
 *   effect type
 * @tparam Context
 *   message context type
 */
final case class ActiveServer[Effect[_], Context] private[automorph] (
  handler: Types.HandlerAnyCodec[Effect, Context],
  transport: ServerMessageTransport[Effect, Context],
) {
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]

  /**
   * Closes this server freeing the underlying resources.
   *
   * @return
   *   passive RPC server
   */
  def close(): Effect[Server[Effect, Context]] =
    genericHandler.system.map(transport.close())(_ => Server(handler, transport))
}
