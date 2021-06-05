package jsonrpc.http.undertow

import io.undertow.io.Receiver
import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.{Headers, StatusCodes}
import java.nio.ByteBuffer
import jsonrpc.JsonRpcHandler
import jsonrpc.core.Protocol
import jsonrpc.core.Protocol.ErrorType
import jsonrpc.http.undertow.UndertowJsonRpcHandler.defaultStatusCodes
import jsonrpc.log.Logging
import jsonrpc.spi.{Codec, Backend}
import jsonrpc.util.EncodingOps.asArraySeq
import jsonrpc.util.EncodingOps.toArraySeq
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * JSON-RPC HTTP handler for Undertow web server.
 *
 * The handler interprets HTTP request body as a JSON-RPC request and processess it using the specified JSON-RPC handler.
 * Subsequent response returned by the JSON-RPC handler is sent as a body of a HTTP response.
 *
 * @see [[https://undertow.io Documentation]]
 * @constructor Create a JSON=RPC HTTP handler for Undertow web server using the specified JSON-RPC ''handler'' and ''effect'' plugin.
 * @param handler JSON-RPC request handler
 * @param effectRunAsync asynchronous effect execution function
 * @param errorStatusCode JSON-RPC error code to HTTP status code mapping function
 * @tparam Effect effect type
 */
final case class UndertowJsonRpcHandler[Effect[_]](
  handler: JsonRpcHandler[?, ?, Effect, HttpServerExchange],
  effectRunAsync: Effect[Any] => Unit,
  errorStatusCode: Int => Int = defaultStatusCodes
) extends HttpHandler with Logging:

  private val backend = handler.backend

  private val receiveCallback = new Receiver.FullBytesCallback:

    override def handle(exchange: HttpServerExchange, request: Array[Byte]): Unit =
      val client = clientAddress(exchange)
      logger.debug("Received HTTP request", Map("Client" -> client))
      exchange.dispatch(new Runnable:

        override def run(): Unit =
          // Process the request
          effectRunAsync(backend.map(
            backend.either(handler.processRequest(request.asArraySeq)(using exchange)),
            _.fold(
              error => sendServerError(error, exchange),
              result =>
                // Send the response
                val response = result.response.getOrElse(ArraySeq.ofByte(Array.empty))
                val statusCode = result.errorCode.map(errorStatusCode).getOrElse(StatusCodes.OK)
                sendResponse(response, statusCode, exchange)
            )
          ))
      )

  override def handleRequest(exchange: HttpServerExchange): Unit =
    // Receive the request
    logger.debug("Receiving HTTP request", Map("Client" -> clientAddress(exchange)))
    Try(exchange.getRequestReceiver.receiveFullBytes(receiveCallback)).recover { case error =>
      sendServerError(error, exchange)
    }.get

  private def sendServerError(error: Throwable, exchange: HttpServerExchange): Unit =
    val statusCode = StatusCodes.INTERNAL_SERVER_ERROR
    val errorMessage = Protocol.errorDetails(error).mkString("\n")
    logger.error("Failed processing HTTP request", error, Map("Client" -> clientAddress(exchange)))
    sendResponse(errorMessage.toArraySeq, statusCode, exchange)

  private def sendResponse(message: ArraySeq.ofByte, statusCode: Int, exchange: HttpServerExchange): Unit =
    if exchange.isResponseChannelAvailable then
      val client = clientAddress(exchange)
      logger.debug("Sending HTTP response", Map("Client" -> client, "Status" -> statusCode.toString))
      exchange.setStatusCode(statusCode).getResponseSender.send(ByteBuffer.wrap(message.unsafeArray))
      logger.debug("Sent HTTP response", Map("Client" -> client, "Status" -> statusCode.toString))

  private def clientAddress(exchange: HttpServerExchange): String =
    val forwardedFor = Option(exchange.getRequestHeaders.get(Headers.X_FORWARDED_FOR_STRING)).map(_.getFirst)
    forwardedFor.map(_.split(",", 2)(0)).getOrElse {
      val address = exchange.getSourceAddress.toString.split("/", 2).reverse.head
      address.replaceAll("/", "").split(":").init.mkString(":")
    }

case object UndertowJsonRpcHandler:

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultStatusCodes = Map(
    ErrorType.ParseError -> StatusCodes.BAD_REQUEST,
    ErrorType.InvalidRequest -> StatusCodes.BAD_REQUEST,
    ErrorType.MethodNotFound -> StatusCodes.NOT_IMPLEMENTED,
    ErrorType.InvalidParams -> StatusCodes.BAD_REQUEST,
    ErrorType.InternalError -> StatusCodes.INTERNAL_SERVER_ERROR,
    ErrorType.IOError -> StatusCodes.INTERNAL_SERVER_ERROR,
    ErrorType.ApplicationError -> StatusCodes.INTERNAL_SERVER_ERROR
  ).withDefaultValue(StatusCodes.INTERNAL_SERVER_ERROR).map((errorType, statusCode) => errorType.code -> statusCode)
