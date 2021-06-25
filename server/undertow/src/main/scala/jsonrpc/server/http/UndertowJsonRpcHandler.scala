package jsonrpc.server.http

import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, StatusCodes}
import java.nio.ByteBuffer
import jsonrpc.Handler
import jsonrpc.handler.HandlerResult
import jsonrpc.log.Logging
import jsonrpc.protocol.ErrorType.ErrorType
import jsonrpc.protocol.ResponseError
import jsonrpc.server.http.UndertowJsonRpcHandler.defaultErrorStatus
import jsonrpc.util.Encoding
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * JSON-RPC HTTP handler for Undertow web server.
 *
 * The handler interprets HTTP request body as a JSON-RPC request and processes it using the specified JSON-RPC handler.
 * The response returned by the JSON-RPC handler is used as HTTP response body.
 *
 * @see [[https://undertow.io Documentation]]
 * @constructor Create a JSON=RPC HTTP handler for Undertow web server using the specified JSON-RPC request ''handler''.
 * @param handler JSON-RPC request handler
 * @param effectRunAsync asynchronous effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status mapping function
 * @tparam Effect effect type
 */
final case class UndertowJsonRpcHandler[Effect[_]](
  handler: Handler[_, _, Effect, HttpServerExchange],
  effectRunAsync: Effect[Any] => Unit,
  errorStatus: Int => Int = defaultErrorStatus
) extends HttpHandler with Logging {

  private val backend = handler.backend

  private val receiveCallback = new Receiver.FullBytesCallback {

    override def handle(exchange: HttpServerExchange, request: Array[Byte]): Unit = {
      val client = clientAddress(exchange)
      logger.debug("Received HTTP request", Map("Client" -> client))
      exchange.dispatch(new Runnable {

        override def run(): Unit =
          // Process the request
          effectRunAsync(backend.map(
            backend.either(handler.processRequest(new ArraySeq.ofByte(request))(exchange)),
            (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) => handlerResult.fold(
              error => sendServerError(error, exchange),
              result => {
                // Send the response
                val response = result.response.getOrElse(new ArraySeq.ofByte(Array.empty))
                val statusCode = result.errorCode.map(errorStatus).getOrElse(StatusCodes.OK)
                sendResponse(response, statusCode, exchange)
              }
            )
          ))
      })
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
    val statusCode = StatusCodes.INTERNAL_SERVER_ERROR
    val message = Encoding.toArraySeq(ResponseError.trace(error).mkString("\n"))
    logger.error("Failed to process HTTP request", error, Map("Client" -> clientAddress(exchange)))
    sendResponse(message, statusCode, exchange)
  }

  private def sendResponse(message: ArraySeq.ofByte, statusCode: Int, exchange: HttpServerExchange): Unit =
    if (exchange.isResponseChannelAvailable) {
      val client = clientAddress(exchange)
      logger.trace("Sending HTTP response", Map("Client" -> client, "Status" -> statusCode.toString))
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, handler.codec.mediaType)
      exchange.setStatusCode(statusCode).getResponseSender.send(ByteBuffer.wrap(message.unsafeArray))
      logger.debug("Sent HTTP response", Map("Client" -> client, "Status" -> statusCode.toString))
    }

  private def clientAddress(exchange: HttpServerExchange): String = {
    val forwardedFor = Option(exchange.getRequestHeaders.get(Headers.X_FORWARDED_FOR_STRING)).map(_.getFirst)
    forwardedFor.map(_.split(",", 2)(0)).getOrElse {
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
