package automorph.transport.websocket.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointMessageTransport}
import automorph.transport.http.{HttpContext, Protocol}
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint.Context
import automorph.util.Extensions.{ByteArrayOps, ByteBufferOps, EffectOps, InputStreamOps, StringOps, ThrowableOps}
import automorph.util.{Network, Random}
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers
import io.undertow.websockets.core.{
  AbstractReceiveListener, BufferedBinaryMessage, BufferedTextMessage, WebSocketCallback, WebSocketChannel, WebSockets,
}
import io.undertow.websockets.spi.WebSocketHttpExchange
import io.undertow.websockets.{WebSocketConnectionCallback, WebSocketProtocolHandshakeHandler}
import java.io.InputStream
import scala.annotation.nowarn
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsJava, MapHasAsScala, SeqHasAsJava}

/**
 * Undertow WebSocket endpoint message transport plugin.
 *
 * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC request
 * handler. The response returned by the RPC request handler is used as WebSocket response message.
 */
object UndertowWebSocketEndpoint {

  /** Request context type. */
  type Context = HttpContext[Either[HttpServerExchange, WebSocketHttpExchange]]

  /**
   * Creates an Undertow WebSocket handler with specified RPC request handler.
   *
   * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC request
   * handler. The response returned by the RPC request handler is used as WebSocket response message.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/WebSocket Transport protocol]]
   * @see
   *   [[https://undertow.io Library documentation]]
   * @see
   *   [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
   * @param handler
   *   RPC request handler
   * @param next
   *   Undertow handler invoked if a HTTP request does not contain a WebSocket handshake
   * @tparam Effect
   *   effect type
   * @return
   *   creates an Undertow WebSocket handler using supplied asynchronous effect execution function
   */
  def apply[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    next: HttpHandler,
  ): WebSocketProtocolHandshakeHandler = {
    val webSocketCallback = UndertowWebSocketCallback(handler)
    new WebSocketProtocolHandshakeHandler(webSocketCallback, next)
  }
}

final private[automorph] case class UndertowWebSocketCallback[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context]
) extends WebSocketConnectionCallback with AutoCloseable with Logging with EndpointMessageTransport {

  private val log = MessageLog(logger, Protocol.WebSocket.name)
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  implicit private val system: EffectSystem[Effect] = genericHandler.system

  override def onConnect(exchange: WebSocketHttpExchange, channel: WebSocketChannel): Unit = {
    val receiveListener = new AbstractReceiveListener {

      override def onFullTextMessage(channel: WebSocketChannel, message: BufferedTextMessage): Unit = {
        val requestBody = message.getData.toInputStream
        handle(exchange, requestBody, channel, () => ())
      }

      override def onFullBinaryMessage(channel: WebSocketChannel, message: BufferedBinaryMessage): Unit = {
        @nowarn
        val data = message.getData
        val requestBody = WebSockets.mergeBuffers(data.getResource*).toInputStream
        handle(exchange, requestBody, channel, () => data.discard())
      }

      private def handle(
        exchange: WebSocketHttpExchange,
        requestBody: InputStream,
        channel: WebSocketChannel,
        discardMessage: () => Unit,
      ): Unit = {
        // Log the request
        val requestId = Random.id
        lazy val requestProperties = getRequestProperties(exchange, requestId)
        log.receivedRequest(requestProperties)

        // Process the request
        genericHandler.processRequest(requestBody, getRequestContext(exchange), requestId).either.map(
          _.fold(
            error => sendErrorResponse(error, exchange, channel, requestId, requestProperties),
            result => {
              // Send the response
              val responseBody = result.responseBody.getOrElse(Array[Byte]().toInputStream)
              sendResponse(responseBody, result.context, exchange, channel, requestId)
              discardMessage()
            },
          )
        ).runAsync
      }

      private def sendErrorResponse(
        error: Throwable,
        exchange: WebSocketHttpExchange,
        channel: WebSocketChannel,
        requestId: String,
        requestProperties: => Map[String, String],
      ): Unit = {
        log.failedProcessRequest(error, requestProperties)
        val responseBody = error.description.toInputStream
        sendResponse(responseBody, None, exchange, channel, requestId)
      }

      private def sendResponse(
        message: InputStream,
        responseContext: Option[Context],
        exchange: WebSocketHttpExchange,
        channel: WebSocketChannel,
        requestId: String,
      ): Unit = {
        // Log the response
        lazy val responseProperties = ListMap(LogProperties.requestId -> requestId, "Client" -> clientAddress(exchange))
        log.sendingResponse(responseProperties)

        // Send the response
        val callback = new WebSocketCallback[Unit] {
          override def complete(channel: WebSocketChannel, context: Unit): Unit =
            log.sentResponse(responseProperties)

          override def onError(channel: WebSocketChannel, context: Unit, error: Throwable): Unit =
            log.failedSendResponse(error, responseProperties)
        }
        setResponseContext(exchange, responseContext)
        WebSockets.sendBinary(message.toByteBuffer, channel, callback, ())
      }

      private def getRequestContext(exchange: WebSocketHttpExchange): Context = {
        val headers = exchange.getRequestHeaders.asScala.view.mapValues(_.asScala).flatMap { case (name, values) =>
          values.map(value => name -> value)
        }.toSeq
        HttpContext(transport = Some(Right(exchange).withLeft[HttpServerExchange]), headers = headers)
          .url(exchange.getRequestURI)
      }

      private def setResponseContext(exchange: WebSocketHttpExchange, responseContext: Option[Context]): Unit = {
        val headers = responseContext.toSeq.flatMap(_.headers).groupBy(_._1).view.mapValues(_.map(_._2).asJava).toMap
          .asJava
        exchange.setResponseHeaders(headers)
      }

      private def getRequestProperties(exchange: WebSocketHttpExchange, requestId: String): Map[String, String] = {
        val query = Option(exchange.getQueryString).filter(_.nonEmpty).map("?" + _).getOrElse("")
        val url = s"${exchange.getRequestURI}$query"
        Map(LogProperties.requestId -> requestId, "Client" -> clientAddress(exchange), "URL" -> url)
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

  override def close(): Unit =
    ()
}
