package automorph.transport.websocket.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.protocol.ResponseError
import automorph.transport.http.Http
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint.Context
import automorph.util.{Bytes, Network}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers
import io.undertow.websockets.core.{AbstractReceiveListener, BufferedBinaryMessage, BufferedTextMessage, WebSocketCallback, WebSocketChannel, WebSockets}
import io.undertow.websockets.spi.WebSocketHttpExchange
import io.undertow.websockets.{WebSocketConnectionCallback, WebSocketProtocolHandshakeHandler}
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}

/**
 * Undertow web server endpoint transport plugin using WebSocket as message transport protocol.
 *
 * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as WebSocket response message.
 */
case object UndertowWebSocketEndpoint {

  /**
   * Creates an Undertow web server WebSocket handler with the specified RPC request ''handler''.
   *
   * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC handler.
   * The response returned by the RPC handler is used as WebSocket response message.
   *
   * @see [[https://undertow.io/ Documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param handler RPC request handler
   * @param runEffect effect execution function
   * @param next Undertow web server handler invoked if a HTTP request does not contain a WebSocket handshake
   * @return Undertow web server HTTP handler
   * @tparam Effect effect type
   */
  def apply[Effect[_]](
    handler: Handler.AnyFormat[Effect, Context],
    runEffect: Effect[Any] => Any,
    next: HttpHandler
  ): WebSocketProtocolHandshakeHandler = {
    val webSocketCallback = UndertowWebSocketCallback(handler, runEffect)
    new WebSocketProtocolHandshakeHandler(webSocketCallback, next)
  }

  /** Request context type. */
  type Context = Http[Either[HttpServerExchange, WebSocketHttpExchange]]
}

/**
 * Undertow web server endpoint transport plugin using WebSocket as message transport protocol.
 *
 * The callback interprets WebSocket request message as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as WebSocket response message.
 *
 * @see [[https://undertow.io/ Documentation]]
 * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
 * @constructor Creates an Undertow web server WebSocket handler with the specified RPC request ''handler''.
 * @param handler RPC request handler
 * @param runEffect effect execution function
 * @tparam Effect effect type
 */
final private[automorph] case class UndertowWebSocketCallback[Effect[_]](
  handler: Handler.AnyFormat[Effect, Context],
  runEffect: Effect[Any] => Any
) extends WebSocketConnectionCallback with AutoCloseable with Logging {

  private val system = handler.system

  override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
    val receiveListener = new AbstractReceiveListener {

      override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage): Unit = {
        val request = Bytes.string.from(message.getData)
        handle(exchange, request, channel, () => ())
      }

      override def onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
        val data = message.getData
        val request = Bytes.byteBuffer.from(WebSockets.mergeBuffers(data.getResource: _*))
        handle(exchange, request, channel, () => data.discard())
      }

      private def handle(
        exchange: WebSocketHttpExchange,
        request: ArraySeq.ofByte,
        channel: WebSocketChannel,
        discardMessage: () => Unit
      ): Unit = {
        val client = clientAddress(exchange)
        logger.debug("Received HTTP request", Map("Client" -> client, "Size" -> request.length))

        // Process the request
        implicit val usingContext: Context = createContext(exchange)
        runEffect(system.map(
          system.either(handler.processRequest(request)),
          (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
            handlerResult.fold(
              error => sendServerError(error, exchange, channel, request),
              result => {
                // Send the response
                val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
                sendResponse(response, exchange, channel)
                discardMessage
              }
            )
        ))
        ()
      }

      private def sendServerError(
        error: Throwable,
        exchange: WebSocketHttpExchange,
        channel: WebSocketChannel,
        request: ArraySeq.ofByte
      ): Unit = {
        logger.error(
          "Failed to process HTTP request",
          error,
          Map("Client" -> clientAddress(exchange), "Size" -> request.length)
        )
        val message = Bytes.string.from(ResponseError.trace(error).mkString("\n"))
        sendResponse(message, exchange, channel)
      }

      private def sendResponse(
        message: ArraySeq.ofByte,
        exchange: WebSocketHttpExchange,
        channel: WebSocketChannel
      ): Unit = {
        val client = clientAddress(exchange)
        logger.trace("Sending HTTP response", Map("Client" -> client, "Size" -> message.length))
        val callback = new WebSocketCallback[Unit] {
          override def complete(channel: WebSocketChannel, context: Unit): Unit =
            logger.debug("Sent HTTP response", Map("Client" -> client, "Size" -> message.length))

          override def onError(channel: WebSocketChannel, context: Unit, throwable: Throwable): Unit =
            logger.error("Failed to send HTTP response", throwable, Map("Client" -> client, "Size" -> message.length))
        }
        WebSockets.sendBinary(Bytes.byteBuffer.to(message), channel, callback, ())
      }

      private def createContext(exchange: WebSocketHttpExchange): Context = {
        val headers = exchange.getRequestHeaders.asScala.view.mapValues(_.asScala).flatMap { case (name, values) =>
          values.map(value => name -> value)
        }.toSeq
        Http(
          source = Some(Right(exchange).withLeft[HttpServerExchange]),
          headers = headers
        ).url(exchange.getRequestURI)
      }

      private def clientAddress(exchange: WebSocketHttpExchange): String = {
        val forwardedFor = Option(exchange.getRequestHeaders.get(Headers.X_FORWARDED_FOR_STRING)).map(_.get(0))
        val address = exchange.getPeerConnections.iterator().next().getSourceAddress.toString
        Network.address(forwardedFor, address)
      }
    }
    channel.getReceiveSetter.set(receiveListener)
    channel.resumeReceives()
  }

  override def close(): Unit = ()

  //  private def sendResponse(channel: WebSocketChannel)(message: Option[JsonRpcMessage]): Unit = {
  //    message match {
  //      case Some(value) => WebSockets.sendText(value.message, channel, null)
  //      case None =>
  //    }
  //  }
}
