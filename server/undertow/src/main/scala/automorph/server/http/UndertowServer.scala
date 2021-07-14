package automorph.server.http

import automorph.log.Logging
import automorph.server.http.UndertowServer.defaultBuilder
import automorph.spi.ServerMessageTransport
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.server.handlers.ResponseCodeHandler
import io.undertow.{Handlers, Undertow}
import java.lang.Runtime
import java.net.InetSocketAddress
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * JSON-RPC server based on Undertow web server using HTTP as message transport protocol.
 *
 * @see [[https://undertow.io/ Documentation]]
 * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
 * @constructor Creates an Undertow web server using the specified HTTP handler.
 * @param httpHandler HTTP handler
 * @param port port to listen on for HTTP connections
 * @param urlPath HTTP URL path (default: /)
 * @param builder Undertow web server builder
 */
final case class UndertowServer(
  httpHandler: HttpHandler,
  port: Int,
  urlPath: String = "/",
  builder: Undertow.Builder = defaultBuilder
) extends ServerMessageTransport with Logging {

  private val undertow = start()

  override def close(): Unit = undertow.stop()

  private def start(): Undertow = {
    // Configure the request handler
    val pathHandler = Handlers.path(ResponseCodeHandler.HANDLE_404).addPrefixPath(urlPath, httpHandler)

    // Configure the web server
    val undertow = builder.addHttpListener(port, "0.0.0.0").setHandler(pathHandler).build()

    // Start the web server
    undertow.getListenerInfo.asScala.foreach { listener =>
      val properties = Map(
        "Protocol" -> listener.getProtcol
      ) ++ (listener.getAddress match {
        case address: InetSocketAddress => Map(
            "Host" -> address.getHostString,
            "Port" -> address.getPort.toString
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
  type Context = HttpServerExchange

  /**
   * Default Undertow web server builder providing the following settings:
   * - IO threads: 2 * number of CPU cores
   * - Worker threads: number of CPU cores
   * - HTTP listener port: 8080
   */
  val defaultBuilder = Undertow.builder()
    .setIoThreads(Runtime.getRuntime.availableProcessors * 2)
    .setWorkerThreads(Runtime.getRuntime.availableProcessors)
}
