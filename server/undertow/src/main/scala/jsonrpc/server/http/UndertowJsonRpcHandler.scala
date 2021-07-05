package jsonrpc.server.http

import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, StatusCodes}
import java.nio.ByteBuffer
import jsonrpc.Handler
import jsonrpc.handler.{Bytes, HandlerResult}
import jsonrpc.log.Logging
import jsonrpc.protocol.{ErrorType, ResponseError}
import jsonrpc.server.http.UndertowJsonRpcHandler.defaultErrorStatus
import jsonrpc.spi.Codec
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * JSON-RPC over HTTP handler for Undertow web server.
 *
 * The handler interprets HTTP request body as a JSON-RPC request and processes it using the specified JSON-RPC handler.
 * The response returned by the JSON-RPC handler is used as HTTP response body.
 *
 * @see [[https://undertow.io Documentation]]
 * @constructor Create a JSON-RPC over HTTP handler for Undertow web server using the specified JSON-RPC request ''handler''.
 * @param handler JSON-RPC request handler
 * @param runEffect effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status mapping function
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 */
final case class UndertowJsonRpcHandler[Node, ExactCodec <: Codec[Node], Effect[_]](
  handler: Handler[Node, ExactCodec, Effect, HttpServerExchange],
  runEffect: Effect[Any] => Unit,
  errorStatus: Int => Int = defaultErrorStatus
) extends HttpHandler with Logging {

  private val backend = handler.backend

  private val receiveCallback = new Receiver.FullBytesCallback {

    override def handle(exchange: HttpServerExchange, request: Array[Byte]): Unit = {
      val client = clientAddress(exchange)
      logger.debug("Received HTTP request", Map("Client" -> client))
      val requestMessage = Bytes.byteArrayBytes.from(request)
      exchange.dispatch(new Runnable {

        override def run(): Unit = {
          // Process the request
          implicit val usingContext = exchange
          runEffect(backend.map(
            backend.either(handler.processRequest(requestMessage)),
            (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
              handlerResult.fold(
                error => sendServerError(error, exchange),
                result => {
                  // Send the response
                  val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
                  val statusCode = result.errorCode.map(errorStatus).getOrElse(StatusCodes.OK)
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
      sendServerError(error, exchange)
    }.get
  }

  private def sendServerError(error: Throwable, exchange: HttpServerExchange): Unit = {
    val message = Bytes.stringBytes.from(ResponseError.trace(error).mkString("\n"))
    val statusCode = StatusCodes.INTERNAL_SERVER_ERROR
    logger.error("Failed to process HTTP request", error, Map("Client" -> clientAddress(exchange)))
    sendResponse(message, statusCode, exchange)
  }

  private def sendResponse(message: ArraySeq.ofByte, statusCode: Int, exchange: HttpServerExchange): Unit = {
    val client = clientAddress(exchange)
    logger.trace("Sending HTTP response", Map("Client" -> client, "Status" -> statusCode.toString))
    if (exchange.isResponseChannelAvailable) {
      exchange.setStatusCode(statusCode).getResponseSender.send(Bytes.byteBufferBytes.to(message))
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, handler.codec.mediaType)
      logger.debug("Sent HTTP response", Map("Client" -> client, "Status" -> statusCode.toString))
    } else {
      logger.error("HTTP response channel not available", Map("Client" -> client, "Status" -> statusCode.toString))
    }
  }

  private def clientAddress(exchange: HttpServerExchange): String = {
    val forwardedFor = Option(exchange.getRequestHeaders.get(Headers.X_FORWARDED_FOR_STRING)).map(_.getFirst)
    forwardedFor.flatMap(_.split(",", 2).headOption).getOrElse {
      val address = exchange.getSourceAddress.toString.split("/", 2).reverse.head
      address.replaceAll("/", "").split(":").init.mkString(":")
    }
  }
}

case object UndertowJsonRpcHandler {

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
