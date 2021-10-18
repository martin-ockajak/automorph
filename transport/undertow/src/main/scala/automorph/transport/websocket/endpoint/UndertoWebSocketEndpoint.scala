package automorph.transport.websocket.endpoint

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint.Context
import automorph.util.Extensions.ThrowableOps
import automorph.util.{Bytes, Network, Random}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers
import io.undertow.websockets.core.{AbstractReceiveListener, BufferedBinaryMessage, BufferedTextMessage, WebSocketCallback, WebSocketChannel, WebSockets}
import io.undertow.websockets.spi.WebSocketHttpExchange
import io.undertow.websockets.{WebSocketConnectionCallback, WebSocketProtocolHandshakeHandler}
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}

/**
 * Undertow web server WebSocket endpoint message transport plugin.
 *
 * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as WebSocket response message.
 */
object UndertowWebSocketEndpoint {

  /**
   * Creates an Undertow web server WebSocket handler with the specified RPC request handler.
   *
   * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC handler.
   * The response returned by the RPC handler is used as WebSocket response message.
   *
   * @see [[https://en.wikipedia.org/wiki/WebSocket Transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param handler RPC request handler
   * @param runEffect executes specified effect asynchronously
   * @param next Undertow web server handler invoked if a HTTP request does not contain a WebSocket handshake
   * @return Undertow web server WebSocket handler
   * @tparam Effect effect type
   */
  def apply[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    runEffect: Effect[Any] => Unit,
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
 * @see [[https://en.wikipedia.org/wiki/WebSocket Transport protocol]]
 * @see [[https://undertow.io/ Library documentation]]
 * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
 * @constructor Creates an Undertow web server WebSocket handler with the specified RPC request handler.
 * @param handler RPC request handler
 * @param runEffect executes specified effect asynchronously
 * @tparam Effect effect type
 */
final private[automorph] case class UndertowWebSocketCallback[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  runEffect: Effect[Any] => Any
) extends WebSocketConnectionCallback with AutoCloseable with Logging with EndpointMessageTransport {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system

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
        val requestId = Random.id
        lazy val requestDetails = requestProperties(exchange, requestId)
        logger.debug("Received WebSocket request", requestDetails)

        // Process the request
        implicit val usingContext: Context = createContext(exchange)
        runEffect(system.map(
          system.either(genericHandler.processRequest(request, requestId)),
          (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
            handlerResult.fold(
              error => sendServerError(error, exchange, channel, requestId, requestDetails),
              result => {
                // Send the response
                val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
                sendResponse(response, exchange, channel, requestId)
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
        requestId: String,
        requestDetails: => Map[String, String]
      ): Unit = {
        logger.error("Failed to process WebSocket request", error, requestDetails)
        val message = Bytes.string.from(error.trace.mkString("\n"))
        sendResponse(message, exchange, channel, requestId)
      }

      private def sendResponse(
        message: ArraySeq.ofByte,
        exchange: WebSocketHttpExchange,
        channel: WebSocketChannel,
        requestId: String
      ): Unit = {
        lazy val responseDetails = Map(
          LogProperties.requestId -> requestId,
          "Client" -> clientAddress(exchange)
        )
        logger.trace("Sending WebSocket response", responseDetails)
        val callback = new WebSocketCallback[Unit] {
          override def complete(channel: WebSocketChannel, context: Unit): Unit =
            logger.debug("Sent WebSocket response", responseDetails)

          override def onError(channel: WebSocketChannel, context: Unit, throwable: Throwable): Unit =
            logger.error("Failed to send WebSocket response", throwable, responseDetails)
        }
        WebSockets.sendBinary(Bytes.byteBuffer.to(message), channel, callback, ())
      }

      private def createContext(exchange: WebSocketHttpExchange): Context = {
        val headers = exchange.getRequestHeaders.asScala.view.mapValues(_.asScala).flatMap { case (name, values) =>
          values.map(value => name -> value)
        }.toSeq
        Http(
          base = Some(Right(exchange).withLeft[HttpServerExchange]),
          headers = headers
        ).url(exchange.getRequestURI)
      }

      private def requestProperties(
        exchange: WebSocketHttpExchange,
        requestId: String
      ): Map[String, String] = Map(
        LogProperties.requestId -> requestId,
        "Client" -> clientAddress(exchange),
        "URL" -> (exchange.getRequestURI + Option(exchange.getQueryString)
          .filter(_.nonEmpty).map("?" + _).getOrElse(""))
      )

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
}
