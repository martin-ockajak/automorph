package automorph.transport.http.server

import automorph.Types
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import automorph.transport.http.server.UndertowServer.{Context, Run}
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint
import io.undertow.predicate.{Predicate, Predicates}
import io.undertow.server.HttpServerExchange
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
 * @param runEffect executes specified effect asynchronously
 * @tparam Effect effect type
 */
final case class UndertowServer[Effect[_]] private (
  handler: Types.HandlerAnyCodec[Effect, Context],
  port: Int,
  path: String,
  methods: Iterable[String],
  webSocket: Boolean,
  mapException: Throwable => Int,
  builder: Undertow.Builder,
  runEffect: Run[Effect]
) extends Logging with ServerMessageTransport[Effect] {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system
  private val allowedMethods = methods.map(_.toUpperCase).toSet
  private lazy val undertow = createServer()

  override def close(): Effect[Unit] =
    system.wrap(undertow.stop())

  private def createServer(): Undertow = {
    // Validate HTTP request method
    val httpHandler = Handlers.predicate(
      new Predicate {
        override def resolve(exchange: HttpServerExchange): Boolean =
          allowedMethods.contains(exchange.getRequestMethod.toString.toUpperCase)
      },
      UndertowHttpEndpoint.create(handler, mapException)(runEffect),
      ResponseCodeHandler.HANDLE_405
    )

    // Validate URL path
    val rootHandler = Handlers.predicate(
      Predicates.prefix(path),
      Option.when(webSocket) {
        UndertowWebSocketEndpoint.create(handler, httpHandler)(runEffect)
      }.getOrElse(httpHandler),
      ResponseCodeHandler.HANDLE_404
    )

    // Start web server
    val undertow = builder.addHttpListener(port, "0.0.0.0").setHandler(rootHandler).build()
    undertow.getListenerInfo.asScala.foreach { listener =>
      val properties = Map(
        "Protocol" -> listener.getProtcol
      ) ++ (listener.getAddress match {
        case address: InetSocketAddress => Map(
            "Host" -> address.getHostString,
            "Port" -> address.getPort
          )
        case _ => Map.empty
      })
      logger.info("Listening for connections", properties)
    }
    undertow
  }
}

object UndertowServer {

  /** Request context type. */
  type Context = UndertowHttpEndpoint.Context

  /**
   * Asynchronous effect execution function type.
   *
   * @tparam Effect effect type
   */
  type Run[Effect[_]] = Effect[Any] => Unit

  /**
   * Creates an Undertow HTTP & WebSocket server with the specified RPC request handler.
   *
   * Resulting function requires:
   * - effect execution function - executes specified effect asynchronously
   *
   * @param handler RPC request handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param methods allowed HTTP request methods (default: POST, GET, PUT, DELETE)
   * @param webSocket support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
   * @param mapException maps an exception to a corresponding HTTP status code
   * @param builder Undertow builder
   * @tparam Effect effect type
   * @return creates an Undertow HTTP & WebSocket server using supplied asynchronous effect execution function
   */
  def create[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    port: Int,
    path: String = "/",
    methods: Iterable[String] = Seq("POST", "GET", "PUT", "DELETE"),
    webSocket: Boolean = true,
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    builder: Undertow.Builder = defaultBuilder
  ): (Run[Effect]) => UndertowServer[Effect] = (runEffect: Run[Effect]) => {
    val server = UndertowServer(handler, port, path, methods, webSocket, mapException, builder, runEffect)
    server.undertow.start()
    server
  }

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
