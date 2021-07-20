package automorph.transport.http.server

import automorph.Handler
import automorph.log.Logging
import automorph.spi.ServerMessageTransport
import automorph.transport.http.endpoint.UndertowHttpEndpoint.defaultErrorStatus
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import automorph.transport.http.server.UndertowServer.{Context, defaultBuilder}
import io.undertow.server.HttpHandler
import io.undertow.server.handlers.ResponseCodeHandler
import io.undertow.{Handlers, Undertow}
import java.net.InetSocketAddress
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * Undertow web server transport plugin using HTTP as message transport protocol.
 *
 * The processs HTTP requests starting with specified URL path using the specified HTTP handler.
 *
 * @see [[https://undertow.io/ Documentation]]
 * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
 * @constructor Creates an Undertow HTTP server with the specified HTTP handler.
 * @param handler RPC request handler
 * @param runEffect effect execution function
 * @param port port to listen on for HTTP connections
 * @param path HTTP URL path (default: /)
 * @param errorStatus JSON-RPC error code to HTTP status mapping function
 * @param webSocket support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
 * @param builder Undertow web server builder
 * @tparam Effect effect type
 */
final case class UndertowServer[Effect[_]](
  handler: Handler.AnyFormat[Effect, Context],
  runEffect: Effect[Any] => Any,
  port: Int,
  path: String = "/",
  errorStatus: Int => Int = defaultErrorStatus,
  webSocket: Boolean = true,
  builder: Undertow.Builder = defaultBuilder
) extends Logging with ServerMessageTransport {

  private val undertow = start()

  override def close(): Unit = undertow.stop()

  private def start(): Undertow = {
    // Configure the request handler
    val httpHandler = UndertowHttpEndpoint(handler, runEffect, errorStatus)
    val webSocketHandler = if (webSocket) {
      UndertowWebSocketEndpoint(handler, runEffect, httpHandler)
    } else {
      httpHandler
    }
    val pathHandler = Handlers.path(ResponseCodeHandler.HANDLE_404).addPrefixPath(path, webSocketHandler)

    // Configure the web server
    val undertow = builder.addHttpListener(port, "0.0.0.0").setHandler(pathHandler).build()

    // Start the web server
    undertow.getListenerInfo.asScala.foreach { listener =>
      val properties = Map(
        "Protocol" -> listener.getProtcol
      ) ++ (listener.getAddress match {
        case address: InetSocketAddress => Map(
            "Host" -> address.getHostString,
            "Port" -> address.getPort
          )
        case _ => Map()
      })
      logger.info("Listening for connections", properties)
    }
    undertow.start()
    undertow
  }
}

case object UndertowServer {
  /** Request context type. */
  type Context = UndertowHttpEndpoint.Context

  /**
   * Default Undertow web server builder providing the following settings:
   * - IO threads: 2 * number of CPU cores
   * - Worker threads: number of CPU cores
   * - HTTP listener port: 8080
   */
  val defaultBuilder: Undertow.Builder = Undertow.builder()
    .setIoThreads(Runtime.getRuntime.availableProcessors * 2)
    .setWorkerThreads(Runtime.getRuntime.availableProcessors)
}
