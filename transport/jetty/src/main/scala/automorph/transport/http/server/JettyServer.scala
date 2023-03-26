package automorph.transport.http.server

import automorph.log.Logging
import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
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
 * Interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * - The response returned by the RPC request handler is used as HTTP response body.
 * - Processes only HTTP requests starting with specified URL path.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://jetty.io Library documentation]]
 * @see
 *   [[https://www.javadoc.io/doc/io.jetty/jetty-core/latest/index.html API]]
 * @constructor
 *   Creates a Jetty HTTP server with specified RPC request handler.
 * @param effectSystem
 *   effect system plugin
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
 * @param handler
 *   RPC request handler
 * @tparam Effect
 *   effect type
 */
final case class JettyServer[Effect[_]](
  effectSystem: EffectSystem[Effect],
  port: Int,
  pathPrefix: String = "/",
  methods: Iterable[HttpMethod] = HttpMethod.values,
  webSocket: Boolean = true,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  threadPool: ThreadPool = new QueuedThreadPool,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy[Effect, Context],
) extends Logging with ServerTransport[Effect, Context] {

  private lazy val jetty = createServer()
  private val allowedMethods = methods.map(_.name).toSet
  private val methodFilter = new Filter {

    override def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain): Unit = {
      val method = request.asInstanceOf[HttpServletRequest].getMethod
      Option.when(allowedMethods.contains(method.toUpperCase))(chain.doFilter(request, response)).getOrElse {
        response.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
      }
    }
  }

  override def clone(handler: RequestHandler[Effect, Context]): JettyServer[Effect] =
    copy(handler = handler)

  override def init(): Effect[Unit] =
    effectSystem.evaluate {
      this.synchronized {
        jetty.start()
        jetty.getConnectors.foreach { connector =>
          connector.getProtocols.asScala.foreach { protocol =>
            logger.info("Listening for connections", ListMap("Protocol" -> protocol, "Port" -> port.toString))
          }
        }
      }
    }

  override def close(): Effect[Unit] =
    effectSystem.evaluate(this.synchronized(jetty.stop()))

  private def createServer(): Server = {
    val endpointTransport = JettyHttpEndpoint(effectSystem, mapException, handler)
    val servletHandler = new ServletContextHandler
    val servletPath = s"$pathPrefix*"

    // Validate URL path
    servletHandler.addServlet(new ServletHolder(endpointTransport), servletPath)

    // Validate HTTP request method
    servletHandler.addFilter(new FilterHolder(methodFilter), servletPath, util.EnumSet.of(DispatcherType.REQUEST))
    val server = new Server(port)
    server.setHandler(servletHandler)
    server
  }
}

object JettyServer {

  /** Request context type. */
  type Context = JettyHttpEndpoint.Context
}
