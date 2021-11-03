package automorph.transport.http.server

import automorph.Types
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.endpoint.JettyHttpEndpoint
import automorph.transport.http.server.JettyServer.Context
import automorph.transport.http.{HttpContext, HttpMethod}
import org.eclipse.jetty.util.thread.{QueuedThreadPool, ThreadPool}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * Jetty HTTP & WebSocket server transport plugin.
 *
 * The server interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as HTTP response body.
 *
 * Processes only HTTP requests starting with specified URL path.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
 * @see [[https://jetty.io Library documentation]]
 * @see [[https://www.javadoc.io/doc/io.jetty/jetty-core/latest/index.html API]]
 * @constructor Creates an Jetty HTTP & WebSocket server with specified RPC request handler.
 * @param handler RPC request handler
 * @param port port to listen on for HTTP connections
 * @param path HTTP URL path
 * @param methods allowed HTTP request methods
 * @param webSocket support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
 * @param mapException maps an exception to a corresponding HTTP status code
 * @param threadPool thread pool
 * @tparam Effect effect type
 */
final case class JettyServer[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  port: Int,
  path: String = "/",
  methods: Iterable[HttpMethod] = HttpMethod.values,
  webSocket: Boolean = true,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  threadPool: ThreadPool = new QueuedThreadPool
) extends Logging with ServerMessageTransport[Effect] {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system
  private val allowedMethods = methods.map(_.name).toSet
  private lazy val jetty = createServer()
  start()

  override def close(): Effect[Unit] =
    system.wrap(jetty.stop())

  private def createServer(): Server = {
    val endpoint = JettyHttpEndpoint(handler, mapException)
    val servletHandler = new ServletContextHandler
    servletHandler.addServlet(new ServletHolder(endpoint), "/*")
    val server = new Server(port)
    server.setHandler(servletHandler)
    server
  }

  private def start(): Unit = {
    jetty.start()
    jetty.getConnectors.foreach { connector =>
      connector.getProtocols.asScala.foreach { protocol =>
        val properties = Map(
          "Protocol" -> protocol,
          "Port" -> port.toString
        )
        logger.info("Listening for connections", properties)
      }
    }
  }
}

object JettyServer {

  /** Request context type. */
  type Context = JettyHttpEndpoint.Context
}
