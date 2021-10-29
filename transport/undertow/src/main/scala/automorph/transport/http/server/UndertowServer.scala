package automorph.transport.http.server

import automorph.Types
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import automorph.transport.http.server.UndertowServer.{Context, RunEffect}
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint
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
 * @param runEffect executes specified effect asynchronously
 * @param port port to listen on for HTTP connections
 * @param path HTTP URL path (default: /)
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @param webSocket support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
 * @param builder Undertow builder
 * @tparam Effect effect type
 */
final case class UndertowServer[Effect[_]] private (
  handler: Types.HandlerAnyCodec[Effect, Context],
  runEffect: RunEffect[Effect],
  port: Int,
  path: String,
  exceptionToStatusCode: Throwable => Int,
  webSocket: Boolean,
  builder: Undertow.Builder
) extends Logging with ServerMessageTransport[Effect] {

  private val undertow = start()
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system

  override def close(): Effect[Unit] =
    system.wrap(undertow.stop())

  private def start(): Undertow = {
    // Configure the request handler
    val httpHandler = UndertowHttpEndpoint.create(handler, exceptionToStatusCode)(runEffect)
    val webSocketHandler = if (webSocket) {
      UndertowWebSocketEndpoint.create(handler, httpHandler)(runEffect)
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
        case _ => Map.empty
      })
      logger.info("Listening for connections", properties)
    }
    undertow.start()
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
  type RunEffect[Effect[_]] = Effect[Any] => Unit

  /**
   * Creates an Undertow HTTP & WebSocket server with the specified HTTP handler.
   *
   * Resulting function requires:
   * - effect execution function - executes specified effect asynchronously
   *
   * @param handler RPC request handler
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @param webSocket support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
   * @param builder Undertow builder
   * @tparam Effect effect type
   * @return creates an Undertow HTTP & WebSocket server using supplied asynchronous effect execution function
   */
  def create[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    webSocket: Boolean = true,
    builder: Undertow.Builder = defaultBuilder
  ): (RunEffect[Effect]) => UndertowServer[Effect] =
    (runEffect: RunEffect[Effect]) =>
      UndertowServer(handler, runEffect, port, path, exceptionToStatusCode, webSocket, builder)

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
