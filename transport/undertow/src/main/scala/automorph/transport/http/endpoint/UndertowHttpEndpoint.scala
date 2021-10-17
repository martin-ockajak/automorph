package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.MessageCodec
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.endpoint.UndertowHttpEndpoint.Context
import automorph.util.Extensions.{ThrowableOps, TryOps}
import automorph.util.{Bytes, Network, Random}
import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, StatusCodes}
import io.undertow.websockets.spi.WebSocketHttpExchange
import java.io.IOException
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}
import scala.util.Try

/**
 * Undertow web server HTTP endpoint message transport plugin.
 *
 * The handler interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://undertow.io Library documentation]]
 * @see [[https://www.javadoc.io/doc/io.undertow/undertow-core/latest/index.html API]]
 * @constructor Creates an Undertow web server HTTP handler with the specified RPC request ''handler''.
 * @param handler RPC request handler
 * @param runEffect executes specified effect asynchronously
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class UndertowHttpEndpoint[Effect[_]](
  handler: Handler.AnyCodec[Effect, Context],
  runEffect: Effect[Any] => Unit,
  exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
) extends HttpHandler with Logging with EndpointMessageTransport {

  private val system = handler.system

  private val receiveCallback = new Receiver.FullBytesCallback {

    override def handle(exchange: HttpServerExchange, message: Array[Byte]): Unit = {
      val requestId = Random.id
      lazy val requestDetails = requestProperties(exchange, requestId)
      logger.debug("Received HTTP request", requestDetails)
      val request = Bytes.byteArray.from(message)
      exchange.dispatch(new Runnable {

        override def run(): Unit = {
          // Process the request
          implicit val usingContext: Context = createContext(exchange)
          runEffect(system.map(
            system.either(handler.processRequest(request, requestId)),
            (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
              handlerResult.fold(
                error => sendServerError(error, exchange, requestId, requestDetails),
                result => {
                  // Send the response
                  val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
                  val statusCode = result.exception.map(exceptionToStatusCode).getOrElse(StatusCodes.OK)
                  sendResponse(response, statusCode, exchange, requestId)
                }
              )
          ))
        }
      })
      ()
    }
  }

  override def handleRequest(exchange: HttpServerExchange): Unit = {
    // Receive the request
    val requestId = Random.id
    lazy val requestDetails = requestProperties(exchange, requestId)
    logger.trace("Receiving HTTP request", requestDetails)
    Try(exchange.getRequestReceiver.receiveFullBytes(receiveCallback)).recover { case error =>
      sendServerError(error, exchange, requestId, requestDetails)
    }.get
  }

  private def sendServerError(
    error: Throwable,
    exchange: HttpServerExchange,
    requestId: String,
    requestDetails: => Map[String, String]
  ): Unit = {
    logger.error("Failed to process HTTP request", error, requestDetails)
    val message = Bytes.string.from(error.trace.mkString("\n"))
    val statusCode = StatusCodes.INTERNAL_SERVER_ERROR
    sendResponse(message, statusCode, exchange, requestId)
  }

  private def sendResponse(
    message: ArraySeq.ofByte,
    statusCode: Int,
    exchange: HttpServerExchange,
    requestId: String
  ): Unit = {
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(exchange),
      "Status" -> statusCode.toString
    )
    logger.trace("Sending HTTP response", responseDetails)
    Try {
      if (exchange.isResponseChannelAvailable) {
        throw new IOException("Response channel not available")
      }
      val mediaType = handler.protocol.codec.asInstanceOf[MessageCodec[_]].mediaType
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, mediaType)
      exchange.setStatusCode(statusCode).getResponseSender.send(Bytes.byteBuffer.to(message))
      logger.debug("Sent HTTP response", responseDetails)
    }.forFailure(logger.error("Failed to send HTTP response", _, responseDetails)).get
  }

  private def createContext(exchange: HttpServerExchange): Context = {
    val headers = exchange.getRequestHeaders.asScala.flatMap { headerValues =>
      headerValues.iterator.asScala.map(value => headerValues.getHeaderName.toString -> value)
    }.toSeq
    Http(
      base = Some(Left(exchange).withRight[WebSocketHttpExchange]),
      method = Some(exchange.getRequestMethod.toString),
      headers = headers
    ).url(exchange.getRequestURI)
  }

  private def requestProperties(
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

  /** Request context type. */
  type Context = Http[Either[HttpServerExchange, WebSocketHttpExchange]]
}
