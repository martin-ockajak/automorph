package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.endpoint.UndertowHttpEndpoint.Context
import automorph.util.Extensions.ThrowableOps
import automorph.util.{Bytes, Network}
import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, StatusCodes}
import io.undertow.websockets.spi.WebSocketHttpExchange
import java.io.IOException
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.{IterableHasAsScala, IteratorHasAsScala}
import scala.util.Try

/**
 * Undertow web server endpoint transport plugin using HTTP as message transport protocol.
 *
 * The handler interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://undertow.io/ Documentation]]
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
      val client = clientAddress(exchange)
      logger.debug("Received HTTP request", Map("Client" -> client, "Size" -> message.length))
      val request = Bytes.byteArray.from(message)
      exchange.dispatch(new Runnable {

        override def run(): Unit = {
          // Process the request
          implicit val usingContext: Context = createContext(exchange)
          runEffect(system.map(
            system.either(handler.processRequest(request)),
            (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
              handlerResult.fold(
                error => sendServerError(error, exchange, Some(request)),
                result => {
                  // Send the response
                  val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
                  val statusCode = result.exception.map(exceptionToStatusCode).getOrElse(StatusCodes.OK)
                  sendResponse(response, statusCode, exchange)
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
    logger.trace("Receiving HTTP request", Map("Client" -> clientAddress(exchange)))
    Try(exchange.getRequestReceiver.receiveFullBytes(receiveCallback)).recover { case error =>
      sendServerError(error, exchange, None)
    }.get
  }

  private def sendServerError(
    error: Throwable,
    exchange: HttpServerExchange,
    request: Option[ArraySeq.ofByte]
  ): Unit = {
    logger.error(
      "Failed to process HTTP request",
      error,
      Map("Client" -> clientAddress(exchange)) ++ request.flatMap(request => Option("Size" -> request.length))
    )
    val message = Bytes.string.from(error.trace.mkString("\n"))
    val statusCode = StatusCodes.INTERNAL_SERVER_ERROR
    sendResponse(message, statusCode, exchange)
  }

  private def sendResponse(message: ArraySeq.ofByte, statusCode: Int, exchange: HttpServerExchange): Unit = {
    val client = clientAddress(exchange)
    logger.trace(
      "Sending HTTP response",
      Map("Client" -> client, "Status" -> statusCode, "Size" -> message.length)
    )
    Try {
      if (exchange.isResponseChannelAvailable) {
        throw new IOException("Response channel not available")
      }
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, handler.protocol.codec.mediaType)
      exchange.setStatusCode(statusCode).getResponseSender.send(Bytes.byteBuffer.to(message))
      logger.debug(
        "Sent HTTP response",
        Map("Client" -> client, "Status" -> statusCode, "Size" -> message.length)
      )
    }.mapFailure { error =>
      logger.error(
        "Failed to send HTTP response",
        error,
        Map("Client" -> client, "Status" -> statusCode, "Size" -> message.length)
      )
      throw error
    }.get
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
