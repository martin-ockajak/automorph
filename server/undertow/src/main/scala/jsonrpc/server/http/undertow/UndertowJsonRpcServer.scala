package jsonrpc.http.undertow

import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.ResponseCodeHandler
import io.undertow.{Handlers, Undertow}
import java.lang.Runtime
import java.net.InetSocketAddress
import jsonrpc.JsonRpcHandler
import jsonrpc.http.undertow.UndertowJsonRpcHandler.defaultStatusCodes
import jsonrpc.http.undertow.UndertowJsonRpcServer.defaultBuilder
import jsonrpc.log.Logging
import jsonrpc.spi.{Codec, Effect}
import scala.jdk.CollectionConverters.ListHasAsScala

/**
 * JSON-RPC HTTP server based on Undertow web server.
 *
 * @see [[https://undertow.io Documentation]]
 * @constructor Create a new JSON=RPC server based on Undertow web server using the specified JSON-RPC ''handler'' and ''effect'' plugin.
 * @param handler JSON-RPC request handler
 * @param effect effect system plugin
 * @param effectRunAsync asynchronous effect execution function
 * @param errorStatusCode JSON-RPC error code to HTTP status code mapping function
 * @param builder Undertow web server builder
 * @param path JSON-RPC API URL path
 * @tparam Node message format node representation type
 * @tparam CodecType message format codec plugin type
 * @tparam Outcome effectful computation outcome type
 */
final case class UndertowJsonRpcServer[Node, CodecType <: Codec[Node], Outcome[_]](
  handler: JsonRpcHandler[Node, CodecType, Outcome, HttpServerExchange],
  effect: Effect[Outcome],
  effectRunAsync: Outcome[Any] => Unit,
  errorStatusCode: Int => Int = defaultStatusCodes,
  builder: Undertow.Builder = defaultBuilder,
  path: String = "/"
) extends AutoCloseable with Logging:

  private val httpHandler =
    UndertowJsonRpcHandler[Node, CodecType, Outcome](handler, effect, effectRunAsync, errorStatusCode)
  private val undertow = build()

  override def close(): Unit =
    undertow.stop()

  private def build(): Undertow =
    // Configure the request handler
    val pathHandler = Handlers.path(ResponseCodeHandler.HANDLE_404)
    pathHandler.addPrefixPath(path, httpHandler)

    // Configure the web server
    val undertow = builder.setHandler(pathHandler).build()

    // Start the web server
    undertow.getListenerInfo.asScala.foreach { listener =>
      val properties = Map(
        "Protocol" -> listener.getProtcol
      ) ++ (listener.getAddress match
        case address: InetSocketAddress => Map(
            "Host" -> address.getHostString,
            "Port" -> address.getPort.toString
          )
        case _ => Map.empty
      )
      logger.info("Listening for connections", properties)
    }
    undertow.start()
    undertow

case object UndertowJsonRpcServer:

  val defaultBuilder = Undertow.builder()
    .setIoThreads(Runtime.getRuntime.availableProcessors * 2)
    .setWorkerThreads(Runtime.getRuntime.availableProcessors)
