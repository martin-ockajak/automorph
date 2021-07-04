package jsonrpc.server.http

import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, StatusCodes}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import jsonrpc.Handler
import jsonrpc.handler.HandlerResult
import jsonrpc.log.Logging
import jsonrpc.protocol.ErrorType
import jsonrpc.protocol.ResponseError
import jsonrpc.server.http.UndertowJsonRpcHandler.defaultErrorStatus
import jsonrpc.spi.Codec
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
 * @param effectRun effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status mapping function
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 */
final case class UndertowJsonRpcHandler[Node, ExactCodec <: Codec[Node], Effect[_]](
  handler: Handler[Node, ExactCodec, Effect, HttpServerExchange],
  effectRun: Effect[Any] => Unit,
  errorStatus: Int => Int = defaultErrorStatus
) extends HttpHandler with Logging {

  private val backend = handler.backend
  private val charset = StandardCharsets.UTF_8

  private val receiveCallback = new Receiver.FullBytesCallback {

    override def handle(exchange: HttpServerExchange, request: Array[Byte]): Unit = {
      val client = clientAddress(exchange)
      logger.debug("Received HTTP request", Map("Client" -> client))
      exchange.dispatch(new Runnable {

        override def run(): Unit = {
          // Process the request
          implicit val usingContext = exchange
          effectRun(backend.map(
            backend.either(handler.processRequest(request)),
            (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
              handlerResult.fold(
                error => sendServerError(error, exchange),
                result => {
                  // Send the response
                  val response = result.response.getOrElse(Array.empty[Byte])
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
    val statusCode = StatusCodes.INTERNAL_SERVER_ERROR
    val message = ResponseError.trace(error).mkString("\n").getBytes(charset)
    logger.error("Failed to process HTTP request", error, Map("Client" -> clientAddress(exchange)))
    sendResponse(message, statusCode, exchange)
  }

  private def sendResponse(message: Array[Byte], statusCode: Int, exchange: HttpServerExchange): Unit =
    if (exchange.isResponseChannelAvailable) {
      val client = clientAddress(exchange)
      logger.trace("Sending HTTP response", Map("Client" -> client, "Status" -> statusCode.toString))
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, handler.codec.mediaType)
      exchange.setStatusCode(statusCode).getResponseSender.send(ByteBuffer.wrap(message))
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
