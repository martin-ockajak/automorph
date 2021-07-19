package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.protocol.{ErrorType, ResponseError}
import automorph.spi.EndpointMessageTransport
import automorph.transport.http.endpoint.UndertowHttpEndpoint.defaultErrorStatus
import automorph.util.{Bytes, Network}
import automorph.util.Extensions.TryOps
import io.undertow.connector.ByteBufferPool
import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange, HttpUpgradeListener}
import io.undertow.util.{AttachmentKey, Headers, StatusCodes}
import io.undertow.websockets.core.WebSocketChannel
import io.undertow.websockets.spi.WebSocketHttpExchange
import java.io.IOException
import java.nio.ByteBuffer
import java.security.Principal
import java.util
import org.xnio.{IoFuture, OptionMap}
import scala.collection.immutable.ArraySeq
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
 * @param runEffect effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status mapping function
 * @tparam Effect effect type
 */
final case class UndertowHttpEndpoint[Effect[_]](
  handler: Handler.AnyFormat[Effect, HttpServerExchange],
  runEffect: Effect[Any] => Any,
  errorStatus: Int => Int = defaultErrorStatus
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
          implicit val usingContext: HttpServerExchange = exchange
          runEffect(system.map(
            system.either(handler.processRequest(request)),
            (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
              handlerResult.fold(
                error => sendServerError(error, exchange, Some(request)),
                result => {
                  // Send the response
                  val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
                  val statusCode = result.errorCode.map(errorStatus).getOrElse(StatusCodes.OK)
                  sendResponse(response, statusCode, exchange)
                }
              )
          ))
          ()
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
    val message = Bytes.string.from(ResponseError.trace(error).mkString("\n"))
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
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, handler.format.mediaType)
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
    }
  }

  private def clientAddress(exchange: HttpServerExchange): String = {
    val forwardedFor = Option(exchange.getRequestHeaders.get(Headers.X_FORWARDED_FOR_STRING)).map(_.getFirst)
    val address = exchange.getSourceAddress.toString
    Network.address(forwardedFor, address)
  }
}

case object UndertowHttpEndpoint {

  /** Request context type. */
  type Context = HttpServerExchange

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus = Map(
    ErrorType.ParseError -> StatusCodes.BAD_REQUEST,
    ErrorType.InvalidRequest -> StatusCodes.BAD_REQUEST,
    ErrorType.MethodNotFound -> StatusCodes.NOT_IMPLEMENTED,
    ErrorType.InvalidParams -> StatusCodes.BAD_REQUEST,
    ErrorType.InternalError -> StatusCodes.INTERNAL_SERVER_ERROR,
    ErrorType.IOError -> StatusCodes.INTERNAL_SERVER_ERROR,
    ErrorType.ApplicationError -> StatusCodes.INTERNAL_SERVER_ERROR
  ).withDefaultValue(StatusCodes.INTERNAL_SERVER_ERROR).map { case (errorType, statusCode) =>
    errorType.code -> statusCode
  }
  
  
}
