package automorph.transport.websocket.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint.{Context, Run}
import automorph.util.Extensions.{EffectOps, ThrowableOps}
import automorph.util.{Bytes, Network, Random}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers
import io.undertow.websockets.core.{AbstractReceiveListener, BufferedBinaryMessage, BufferedTextMessage, WebSocketCallback, WebSocketChannel, WebSockets}
import io.undertow.websockets.spi.WebSocketHttpExchange
import io.undertow.websockets.{WebSocketConnectionCallback, WebSocketProtocolHandshakeHandler}
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}

/**
 * Undertow WebSocket endpoint message transport plugin.
 *
 * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as WebSocket response message.
 */
object UndertowWebSocketEndpoint {

  /**
   * Asynchronous effect execution function type.
   *
   * @tparam Effect effect type
   */
  type Run[Effect[_]] = Effect[Any] => Unit

  /**
   * Creates an Undertow WebSocket handler with specified RPC request handler.
   *
   * Resulting function requires:
   * - effect execution function - executes specified effect asynchronously
   *
   * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC request handler.
   * The response returned by the RPC request handler is used as WebSocket response message.
   *
   * @see [[https://en.wikipedia.org/wiki/WebSocket Transport protocol]]
   * @see [[https://undertow.io Library documentation]]
   * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param handler RPC request handler
   * @param next Undertow handler invoked if a HTTP request does not contain a WebSocket handshake
   * @tparam Effect effect type
   * @return creates an Undertow WebSocket handler using supplied asynchronous effect execution function
   */
  def create[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    next: HttpHandler
  ): (Run[Effect]) => WebSocketProtocolHandshakeHandler = (runEffect: Run[Effect]) => {
    val webSocketCallback = UndertowWebSocketCallback(handler, runEffect)
    new WebSocketProtocolHandshakeHandler(webSocketCallback, next)
  }

  /** Request context type. */
  type Context = HttpContext[Either[HttpServerExchange, WebSocketHttpExchange]]
}

final private[automorph] case class UndertowWebSocketCallback[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  runEffect: Run[Effect]
) extends WebSocketConnectionCallback with AutoCloseable with Logging with EndpointMessageTransport {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system
  implicit private val givenSystem: EffectSystem[Effect] = system

  override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
    val receiveListener = new AbstractReceiveListener {

      override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage): Unit = {
        val requestBody = Bytes.string.from(message.getData)
        handle(exchange, requestBody, channel, () => ())
      }

      override def onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
        val data = message.getData
        val requestBody = Bytes.byteBuffer.from(WebSockets.mergeBuffers(data.getResource: _*))
        handle(exchange, requestBody, channel, () => data.discard())
      }

      private def handle(
        exchange: WebSocketHttpExchange,
        requestBody: ArraySeq.ofByte,
        channel: WebSocketChannel,
        discardMessage: () => Unit
      ): Unit = {
        // Log the request
        val requestId = Random.id
        lazy val requestProperties = extractRequestProperties(exchange, requestId)
        logger.debug("Received WebSocket request", requestProperties)

        // Process the request
        implicit val givenContext: Context = requestContext(exchange)
        runEffect(genericHandler.processRequest(requestBody, requestId, None).either.map(_.fold(
          error => sendErrorResponse(error, exchange, channel, requestId, requestProperties),
          result => {
            // Send the response
            val response = result.responseBody.getOrElse(new ArraySeq.ofByte(Array()))
            sendResponse(response, exchange, channel, requestId)
            discardMessage
          }
        )))
        ()
      }

      private def sendErrorResponse(
        error: Throwable,
        exchange: WebSocketHttpExchange,
        channel: WebSocketChannel,
        requestId: String,
        requestProperties: => Map[String, String]
      ): Unit = {
        logger.error("Failed to process WebSocket request", error, requestProperties)
        val message = Bytes.string.from(error.trace.mkString("\n"))
        sendResponse(message, exchange, channel, requestId)
      }

      private def sendResponse(
        message: ArraySeq.ofByte,
        exchange: WebSocketHttpExchange,
        channel: WebSocketChannel,
        requestId: String
      ): Unit = {
        // Log the response
        lazy val responseDetails = Map(
          LogProperties.requestId -> requestId,
          "Client" -> clientAddress(exchange)
        )
        logger.trace("Sending WebSocket response", responseDetails)

        // Send the response
        val callback = new WebSocketCallback[Unit] {
          override def complete(channel: WebSocketChannel, context: Unit): Unit =
            logger.debug("Sent WebSocket response", responseDetails)

          override def onError(channel: WebSocketChannel, context: Unit, throwable: Throwable): Unit =
            logger.error("Failed to send WebSocket response", throwable, responseDetails)
        }
        WebSockets.sendBinary(Bytes.byteBuffer.to(message), channel, callback, ())
      }

      private def requestContext(exchange: WebSocketHttpExchange): Context = {
        val headers = exchange.getRequestHeaders.asScala.view.mapValues(_.asScala).flatMap { case (name, values) =>
          values.map(value => name -> value)
        }.toSeq
        HttpContext(
          base = Some(Right(exchange).withLeft[HttpServerExchange]),
          headers = headers
        ).url(exchange.getRequestURI)
      }

      private def extractRequestProperties(
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
