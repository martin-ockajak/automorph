package automorph.transport.http.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.endpoint.UndertowHttpEndpoint.Context
import automorph.transport.http.{HttpContext, HttpLog, HttpMethod, Protocol}
import automorph.util.Extensions.{EffectOps, ThrowableOps, TryOps}
import automorph.util.{Bytes, Network, Random}
import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, HttpString, StatusCodes}
import io.undertow.websockets.spi.WebSocketHttpExchange
import java.io.IOException
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}
import scala.util.Try

/**
 * Undertow HTTP endpoint message transport plugin.
 *
 * The handler interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://undertow.io Library documentation]]
 * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
 * @constructor Creates an Undertow HTTP handler with specified RPC request handler.
 * @param handler RPC request handler
 * @param mapException maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class UndertowHttpEndpoint[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode
) extends HttpHandler with Logging with EndpointMessageTransport {

  private val log = HttpLog(logger, Protocol.Http)
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  implicit private val system: EffectSystem[Effect] = genericHandler.system

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    // Log the request
    val requestId = Random.id
    lazy val requestProperties = getRequestProperties(exchange, requestId)
    log.receivingRequest(requestProperties)
    val receiveCallback = new Receiver.FullBytesCallback {

      override def handle(exchange: HttpServerExchange, message: Array[Byte]): Unit = {
        log.receivedRequest(requestProperties)
        val requestBody = Bytes.byteArray.from(message)
        val handlerRunnable = new Runnable {

          override def run(): Unit = {
            // Process the request
            genericHandler.processRequest(requestBody, getRequestContext(exchange), requestId).either.map(_.fold(
              error => sendErrorResponse(error, exchange, requestId, requestProperties),
              result => {
                // Send the response
                val responseBody = result.responseBody.getOrElse(new ArraySeq.ofByte(Array()))
                val statusCode = result.exception.map(mapException).getOrElse(StatusCodes.OK)
                sendResponse(responseBody, statusCode, result.context, exchange, requestId)
              }
            )).run
          }
        }
        if (exchange.isInIoThread) {
          exchange.dispatch(handlerRunnable)
          ()
        } else {
          handlerRunnable.run()
        }
      }
    }
    Try(exchange.getRequestReceiver.receiveFullBytes(receiveCallback)).recover { case error =>
      sendErrorResponse(error, exchange, requestId, requestProperties)
    }.get
  }

  private def sendErrorResponse(
    error: Throwable,
    exchange: HttpServerExchange,
    requestId: String,
    requestProperties: => Map[String, String]
  ): Unit = {
    log.failedProcessing(error, requestProperties)
    val responseBody = Bytes.string.from(error.trace.mkString("\n"))
    val statusCode = StatusCodes.INTERNAL_SERVER_ERROR
    sendResponse(responseBody, statusCode, None, exchange, requestId)
  }

  private def sendResponse(
    responseBody: ArraySeq.ofByte,
    statusCode: Int,
    responseContext: Option[Context],
    exchange: HttpServerExchange,
    requestId: String
  ): Unit = {
    // Log the response
    val responseStatusCode = responseContext.flatMap(_.statusCode).getOrElse(statusCode)
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(exchange),
      "Status" -> responseStatusCode.toString
    )
    log.sendingResponse(responseProperties)

    // Send the response
    Try {
      if (!exchange.isResponseChannelAvailable) {
        throw new IOException("Response channel not available")
      }
      setResponseContext(exchange, responseContext)
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, genericHandler.protocol.codec.mediaType)
      exchange.setStatusCode(responseStatusCode).getResponseSender.send(Bytes.byteBuffer.to(responseBody))
      log.sentResponse(responseProperties)
    }.onFailure { error =>
      log.failedResponse(error, responseProperties)
    }.get
  }

  private def getRequestContext(exchange: HttpServerExchange): Context = {
    val headers = exchange.getRequestHeaders.asScala.flatMap { headerValues =>
      headerValues.iterator.asScala.map(value => headerValues.getHeaderName.toString -> value)
    }.toSeq
    HttpContext(
      transport = Some(Left(exchange).withRight[WebSocketHttpExchange]),
      method = Some(HttpMethod.valueOf(exchange.getRequestMethod.toString)),
      headers = headers
    ).url(exchange.getRequestURI)
  }

  private def setResponseContext(exchange: HttpServerExchange, responseContext: Option[Context]): Unit = {
    val responseHeaders = exchange.getResponseHeaders
    responseContext.toSeq.flatMap(_.headers).foreach { case (name, value) =>
      responseHeaders.add(new HttpString(name), value)
    }
  }

  private def getRequestProperties(exchange: HttpServerExchange, requestId: String): Map[String, String] = {
    val url = exchange.getRequestURI + Option(exchange.getQueryString).filter(_.nonEmpty).map("?" + _).getOrElse("")
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(exchange),
      "URL" -> url,
      "Method" -> exchange.getRequestMethod.toString
    )
  }

  private def clientAddress(exchange: HttpServerExchange): String = {
    val forwardedFor = Option(exchange.getRequestHeaders.get(Headers.X_FORWARDED_FOR_STRING)).map(_.getFirst)
    val address = exchange.getSourceAddress.toString
    Network.address(forwardedFor, address)
  }
}

object UndertowHttpEndpoint {

  /** Request context type. */
  type Context = HttpContext[Either[HttpServerExchange, WebSocketHttpExchange]]
}
