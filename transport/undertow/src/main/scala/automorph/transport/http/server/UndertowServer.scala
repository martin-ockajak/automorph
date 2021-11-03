package automorph.transport.http.server

import automorph.Types
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import automorph.transport.http.server.UndertowServer.{Context, defaultBuilder}
import automorph.transport.http.{HttpContext, HttpMethod}
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint
import io.undertow.predicate.{Predicate, Predicates}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.server.handlers.ResponseCodeHandler
import io.undertow.{Handlers, Undertow}
import java.net.InetSocketAddress
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * Undertow HTTP & WebSocket server transport plugin.
 *
 * The server interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as HTTP response body.
 *
 * Processes only HTTP requests starting with specified URL path.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://en.wikipedia.org/wiki/WebSocket Alternative transport protocol]]
 * @see [[https://undertow.io Library documentation]]
 * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
 * @constructor Creates an Undertow HTTP & WebSocket server with specified RPC request handler.
 * @param handler RPC request handler
 * @param port port to listen on for HTTP connections
 * @param path HTTP URL path
 * @param methods allowed HTTP request methods
 * @param webSocket support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
 * @param mapException maps an exception to a corresponding HTTP status code
 * @param builder Undertow builder
 * @tparam Effect effect type
 */
final case class UndertowServer[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  port: Int,
  path: String = "/",
  methods: Iterable[HttpMethod] = HttpMethod.values,
  webSocket: Boolean = true,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  builder: Undertow.Builder = defaultBuilder
) extends Logging with ServerMessageTransport[Effect] {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system
  private val allowedMethods = methods.map(_.name).toSet
  private lazy val undertow = createServer()
  start()

  override def close(): Effect[Unit] =
    system.wrap(undertow.stop())

  private def createServer(): Undertow = {
    // Validate HTTP request method
    val httpHandler = methodHandler(UndertowHttpEndpoint(handler, mapException))

    // Validate URL path
    val rootHandler = Handlers.predicate(
      Predicates.prefix(path),
      Option.when(webSocket)(UndertowWebSocketEndpoint(handler, httpHandler)).getOrElse(httpHandler),
      ResponseCodeHandler.HANDLE_404
    )
    builder.addHttpListener(port, "0.0.0.0", rootHandler).build()
  }

  private def start(): Unit = {
    undertow.start()
    undertow.getListenerInfo.asScala.foreach { listener =>
      val properties = Map(
        "Protocol" -> listener.getProtcol
      ) ++ (listener.getAddress match {
        case address: InetSocketAddress => Map(
            "Host" -> address.getHostString,
            "Port" -> address.getPort.toString
          )
        case _ => Map.empty
      })
      logger.info("Listening for connections", properties)
    }
  }

  private def methodHandler(handler: HttpHandler): HttpHandler =
    Handlers.predicate(
      new Predicate {

        override def resolve(exchange: HttpServerExchange): Boolean =
          allowedMethods.contains(exchange.getRequestMethod.toString.toUpperCase)
      },
      handler,
      ResponseCodeHandler.HANDLE_405
    )
}

object UndertowServer {

  /** Request context type. */
  type Context = UndertowHttpEndpoint.Context

  /**
   * Default Undertow web server builder providing the following settings:
   * - IO threads: 2 * number of CPU cores
   * - Worker threads: number of CPU cores
   * - HTTP listener port: 8080
   */
  def defaultBuilder: Undertow.Builder = Undertow.builder()
    .setIoThreads(Runtime.getRuntime.availableProcessors * 2)
    .setWorkerThreads(Runtime.getRuntime.availableProcessors)
}
