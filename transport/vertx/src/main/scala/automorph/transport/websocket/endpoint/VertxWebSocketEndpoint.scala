package automorph.transport.websocket.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.{HttpContext, MessageLog, Protocol}
import automorph.transport.websocket.endpoint.VertxWebSocketEndpoint.Context
import automorph.util.Extensions.{EffectOps, ThrowableOps}
import automorph.util.{Bytes, Network, Random}
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpServerRequest, ServerWebSocket}
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala}

/**
 * Vert.x WebSocket endpoint message transport plugin.
 *
 * The handler interprets WebSocket request message as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as WebSocket response message.
 *
 * @see [[https://en.wikipedia.org/wiki/WebSocket Transport protocol]]
 * @see [[https://vertx.io Library documentation]]
 * @see [[https://vertx.io/docs/apidocs/index.html API]]
 * @constructor Creates an Vert.x WebSocket handler with specified RPC request handler.
 * @param handler RPC request handler
 * @tparam Effect effect type
 */
final case class VertxWebSocketEndpoint[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context]
) extends Handler[ServerWebSocket] with Logging with EndpointMessageTransport {

  private val headerXForwardedFor = "X-Forwarded-For"
  private val log = MessageLog(logger, Protocol.WebSocket.name)
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  implicit private val system: EffectSystem[Effect] = genericHandler.system

  override def handle(request: ServerWebSocket): Unit = {
    // Log the request
    val requestId = Random.id
    lazy val requestProperties = getRequestProperties(request, requestId)
    log.receivingRequest(requestProperties)
    request.binaryMessageHandler { buffer =>
      val requestBody = Bytes.byteArray.from(buffer.getBytes)
      log.receivedRequest(requestProperties)

      // Process the request
      genericHandler.processRequest(requestBody, getRequestContext(request), requestId).either.map(_.fold(
        error => sendErrorResponse(error, request, requestId, requestProperties),
        result => {
          // Send the response
          val responseBody = result.responseBody.getOrElse(new ArraySeq.ofByte(Array()))
          sendResponse(responseBody, request, requestId)
        }
      )).run
    }
    ()
  }

  private def sendErrorResponse(
    error: Throwable,
    request: ServerWebSocket,
    requestId: String,
    requestProperties: => Map[String, String]
  ): Unit = {
    log.failedProcessRequest(error, requestProperties)
    val responseBody = Bytes.string.from(error.trace.mkString("\n"))
    sendResponse(responseBody, request, requestId)
  }

  private def sendResponse(
    responseBody: ArraySeq.ofByte,
    request: ServerWebSocket,
    requestId: String
  ): Unit = {
    // Log the response
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request)
    )
    log.sendingResponse(responseProperties)

    // Send the response
    request.writeBinaryMessage(Buffer.buffer(Bytes.byteArray.to(responseBody))).onSuccess { _ =>
      log.sentResponse(responseProperties)
    }.onFailure { error =>
      log.failedSendResponse(error, responseProperties)
    }
    ()
  }

  private def getRequestContext(request: ServerWebSocket): Context = {
    val headers = request.headers.entries.asScala.map { entry =>
      entry.getKey -> entry.getValue
    }.toSeq
    HttpContext(
      transport = Some(Right(request).withLeft[HttpServerRequest]),
      headers = headers
    ).url(request.uri)
  }

  private def getRequestProperties(request: ServerWebSocket, requestId: String): Map[String, String] = {
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "URL" -> request.uri
    )
  }

  private def clientAddress(request: ServerWebSocket): String = {
    val forwardedFor = Option(request.headers().get(headerXForwardedFor))
    val address = Option(request.remoteAddress.hostName).orElse(Option(request.remoteAddress.hostAddress)).getOrElse("")
    Network.address(forwardedFor, address)
  }
}

object VertxWebSocketEndpoint {

  /** Request context type. */
  type Context = HttpContext[Either[HttpServerRequest, ServerWebSocket]]
}
