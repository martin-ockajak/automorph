package automorph.transport.http.server

import automorph.Types
import automorph.log.Logging
import automorph.spi.ServerMessageTransport
import automorph.transport.http.endpoint.JettyHttpEndpoint
import automorph.transport.http.server.JettyServer.Context
import automorph.transport.http.{HttpContext, HttpMethod}
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import jakarta.servlet.{DispatcherType, Filter, FilterChain, ServletRequest, ServletResponse}
import java.util
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{FilterHolder, ServletContextHandler, ServletHolder}
import org.eclipse.jetty.util.thread.{QueuedThreadPool, ThreadPool}
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * Jetty HTTP server message transport plugin.
 *
 * The server interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://jetty.io Library documentation]]
 * @see
 *   [[https://www.javadoc.io/doc/io.jetty/jetty-core/latest/index.html API]]
 * @constructor
 *   Creates and starts a Jetty HTTP server with specified RPC request handler.
 * @param handler
 *   RPC request handler
 * @param port
 *   port to listen on for HTTP connections
 * @param pathPrefix
 *   HTTP URL path prefix, only requests starting with this path prefix are allowed
 * @param methods
 *   allowed HTTP request methods
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @param threadPool
 *   thread pool
 * @tparam Effect
 *   effect type
 */
final case class JettyServer[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  port: Int,
  pathPrefix: String = "/",
  methods: Iterable[HttpMethod] = HttpMethod.values,
  webSocket: Boolean = true,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  threadPool: ThreadPool = new QueuedThreadPool,
) extends Logging with ServerMessageTransport[Effect, Context] {

  private lazy val jetty = createServer()
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.effectSystem
  private val allowedMethods = methods.map(_.name).toSet
  private val methodFilter = new Filter {

    override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
      val method = request.asInstanceOf[HttpServletRequest].getMethod
      Option.when(allowedMethods.contains(method.toUpperCase))(chain.doFilter(request, response)).getOrElse {
        response.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
      }
    }
  }
  start()

  override def close(): Effect[Unit] =
    system.evaluate(jetty.stop())

  private def createServer(): Server = {
    val endpoint = JettyHttpEndpoint(handler, mapException)
    val servletHandler = new ServletContextHandler
    val servletPath = s"$pathPrefix*"

    // Validate URL path
    servletHandler.addServlet(new ServletHolder(endpoint), servletPath)

    // Validate HTTP request method
    servletHandler.addFilter(new FilterHolder(methodFilter), servletPath, util.EnumSet.of(DispatcherType.REQUEST))
    val server = new Server(port)
    server.setHandler(servletHandler)
    server
  }

  private def start(): Unit = {
    jetty.start()
    jetty.getConnectors.foreach { connector =>
      connector.getProtocols.asScala.foreach { protocol =>
        logger.info("Listening for connections", ListMap("Protocol" -> protocol, "Port" -> port.toString))
      }
    }
  }
}

object JettyServer {

  /** Request context type. */
  type Context = JettyHttpEndpoint.Context
}
