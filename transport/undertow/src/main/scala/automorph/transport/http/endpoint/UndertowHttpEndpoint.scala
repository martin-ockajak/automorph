package automorph.transport.http.endpoint

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.http.endpoint.UndertowHttpEndpoint.Context
import automorph.transport.websocket.endpoint.UndertowWebSocketEndpoint.RunEffect
import automorph.util.Extensions.{ThrowableOps, TryOps}
import automorph.util.{Bytes, Network, Random}
import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, HttpString, StatusCodes}
import io.undertow.websockets.spi.WebSocketHttpExchange
import java.io.IOException
import scala.collection.immutable.ArraySeq
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
 * @param runEffect executes specified effect asynchronously
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class UndertowHttpEndpoint[Effect[_]] private (
  handler: Types.HandlerAnyCodec[Effect, Context],
  runEffect: RunEffect[Effect],
  exceptionToStatusCode: Throwable => Int
) extends HttpHandler with Logging with EndpointMessageTransport {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system

  private val receiveCallback = new Receiver.FullBytesCallback {

    override def handle(exchange: HttpServerExchange, message: Array[Byte]): Unit = {
      // Log the request
      val requestId = Random.id
      lazy val requestProperties = extractRequestProperties(exchange, requestId)
      logger.debug("Received HTTP request", requestProperties)
      val requestBody = Bytes.byteArray.from(message)
      exchange.dispatch(new Runnable {

        override def run(): Unit = {
          // Process the request
          implicit val usingContext: Context = requestContext(exchange)
          runEffect(system.map(
            system.either(genericHandler.processRequest(requestBody, requestId, Some(exchange.getRequestPath))),
            (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte, Context]]) =>
              handlerResult.fold(
                error => sendErrorResponse(error, exchange, requestId, requestProperties),
                result => {
                  // Send the response
                  val response = result.responseBody.getOrElse(new ArraySeq.ofByte(Array()))
                  val statusCode = result.exception.map(exceptionToStatusCode).getOrElse(StatusCodes.OK)
                  sendResponse(response, statusCode, result.context, exchange, requestId)
                }
              )
          ))
        }
      })
      ()
    }
  }

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    val requestId = Random.id
    lazy val requestProperties = extractRequestProperties(exchange, requestId)
    logger.trace("Receiving HTTP request", requestProperties)
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
    logger.error("Failed to process HTTP request", error, requestProperties)
    val message = Bytes.string.from(error.trace.mkString("\n"))
    val statusCode = StatusCodes.INTERNAL_SERVER_ERROR
    sendResponse(message, statusCode, None, exchange, requestId)
  }

  private def sendResponse(
    message: ArraySeq.ofByte,
    statusCode: Int,
    responseContext: Option[Context],
    exchange: HttpServerExchange,
    requestId: String
  ): Unit = {
    // Log the response
    val responseStatusCode = responseContext.flatMap(_.statusCode).getOrElse(statusCode)
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(exchange),
      "Status" -> responseStatusCode.toString
    )
    logger.trace("Sending HTTP response", responseDetails)

    // Send the response
    Try {
      if (exchange.isResponseChannelAvailable) {
        throw new IOException("Response channel not available")
      }
      val mediaType = genericHandler.protocol.codec.mediaType
      exchange.setStatusCode(responseStatusCode).getResponseSender.send(Bytes.byteBuffer.to(message))
      val responseHeaders = exchange.getResponseHeaders
      responseContext.toSeq.flatMap(_.headers).foreach { case (name, value) =>
        responseHeaders.add(new HttpString(name), value)
      }
      responseHeaders.put(Headers.CONTENT_TYPE, mediaType)
      logger.debug("Sent HTTP response", responseDetails)
    }.onFailure(logger.error("Failed to send HTTP response", _, responseDetails)).get
  }

  private def requestContext(exchange: HttpServerExchange): Context = {
    val headers = exchange.getRequestHeaders.asScala.flatMap { headerValues =>
      headerValues.iterator.asScala.map(value => headerValues.getHeaderName.toString -> value)
    }.toSeq
    HttpContext(
      base = Some(Left(exchange).withRight[WebSocketHttpExchange]),
      method = Some(exchange.getRequestMethod.toString),
      headers = headers
    ).url(exchange.getRequestURI)
  }

  private def extractRequestProperties(
    exchange: HttpServerExchange,
    requestId: String
  ): Map[String, String] = Map(
    LogProperties.requestId -> requestId,
    "Client" -> clientAddress(exchange),
    "URL" -> (exchange.getRequestURI + Option(exchange.getQueryString)
      .filter(_.nonEmpty).map("?" + _).getOrElse("")),
    "Method" -> exchange.getRequestMethod.toString
  )

  private def clientAddress(exchange: HttpServerExchange): String = {
    val forwardedFor = Option(exchange.getRequestHeaders.get(Headers.X_FORWARDED_FOR_STRING)).map(_.getFirst)
    val address = exchange.getSourceAddress.toString
    Network.address(forwardedFor, address)
  }
}

object UndertowHttpEndpoint {

  /**
   * Asynchronous effect execution function type.
   *
   * @tparam Effect effect type
   */
  type RunEffect[Effect[_]] = Effect[Any] => Unit

  /**
   * Creates an Undertow HTTP endpoint message transport plugin with specified RPC request handler.
   *
   * Resulting function requires:
   * - effect execution function - executes specified effect asynchronously
   *
   * @param handler RPC request handler
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @tparam Effect effect type
   * @return creates an Undertow HTTP handler using supplied asynchronous effect execution function
   */
  def create[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    exceptionToStatusCode: Throwable => Int = HttpContext.defaultExceptionToStatusCode
  ): RunEffect[Effect] => UndertowHttpEndpoint[Effect] = (runEffect: RunEffect[Effect]) =>
    UndertowHttpEndpoint(handler, runEffect, exceptionToStatusCode)

  /** Request context type. */
  type Context = HttpContext[Either[HttpServerExchange, WebSocketHttpExchange]]
}
