package jsonrpc.server.http.standard

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import jsonrpc.Handler
import jsonrpc.handler.HandlerResult
import jsonrpc.log.Logging
import jsonrpc.protocol.Errors
import jsonrpc.protocol.Errors.ErrorType
import jsonrpc.server.http.standard.NanoHTTPD.Response.Status
import jsonrpc.server.http.standard.NanoHTTPD.{IHTTPSession, Response, newFixedLengthResponse}
import jsonrpc.server.http.standard.NanoHttpdServer.defaultErrorStatus
import jsonrpc.util.EncodingOps.toArraySeq
import scala.collection.immutable.ArraySeq

/**
 * HTTP server based on NanoHTTPD web server.
 *
 * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
 * @constructor Create an NanoHTTPD web server using the specified JSON-RPC request ''handler''.
 * @param handler JSON-RPC request handler
 * @param effectRunSync synchronous effect execution function
 * @param port port to listen on for HTTP connections
 * @param readTimeout HTTP connection read timeout (milliseconds)
 * @param errorStatus JSON-RPC error code to HTTP status mapping function
 * @tparam Effect effect type
 */
case class NanoHttpdServer[Effect[_]] private (
  handler: Handler[?, ?, Effect, IHTTPSession],
  effectRunSync: Effect[Response] => Response,
  port: Int,
  readTimeout: Int,
  errorStatus: Int => Status
) extends NanoHTTPD(port) with AutoCloseable with Logging:

  private val HeaderXForwardedFor = "X-Forwarded-For"
  private val backend = handler.backend

  override def start(): Unit =
    logger.info("Listening for connections", Map("Port" -> port.toString))
    super.start()

  override def serve(session: IHTTPSession): Response =
    // Receive the request
    logger.trace("Receiving HTTP request", Map("Client" -> clientAddress(session)))
    val request = session.getInputStream.toArraySeq(handler.bufferSize)

    // Process the request
    effectRunSync(backend.map(
      backend.either(handler.processRequest(request)(using session)),
      _.fold(
        error => createServerError(error, session),
        result =>
          // Send the response
          val response = result.response.getOrElse(ArraySeq.ofByte(Array.empty))
          val status = result.errorCode.map(errorStatus).getOrElse(Status.OK)
          createResponse(response, status, session)
      )
    ))

  private def createServerError(error: Throwable, session: IHTTPSession): Response =
    val status = Status.INTERNAL_ERROR
    val errorMessage = Errors.errorDetails(error).mkString("\n").toArraySeq
    logger.error("Failed to process HTTP request", error, Map("Client" -> clientAddress(session)))
    createResponse(errorMessage, status, session)

  private def createResponse(message: ArraySeq.ofByte, status: Status, session: IHTTPSession): Response =
    val client = clientAddress(session)
    logger.trace("Sending HTTP response", Map("Client" -> client, "Status" -> status.getRequestStatus.toString))
    val inputStream = ByteArrayInputStream(message.unsafeArray)
    val response = newFixedLengthResponse(status, handler.codec.mediaType, inputStream, message.size)
    logger.debug("Sent HTTP response", Map("Client" -> client, "Status" -> status.getRequestStatus.toString))
    response

  private def clientAddress(session: IHTTPSession): String =
    val forwardedFor = Option(session.getHeaders.get(HeaderXForwardedFor))
    forwardedFor.map(_.split(",", 2)(0)).getOrElse {
      session.getRemoteHostName
    }

  override def close(): Unit =
    stop()

case object NanoHttpdServer:

  /**
   * Create an NanoHTTPD web server using the specified JSON-RPC request ''handler''.
   *
   * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
   * @param handler JSON-RPC request handler
   * @param effectRunSync synchronous effect execution function
   * @param port port to listen on for HTTP connections
   * @param readTimeout HTTP connection read timeout (milliseconds)
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   */
  def apply[Effect[_]](
    handler: Handler[?, ?, Effect, IHTTPSession],
    effectRunSync: Effect[Response] => Response,
    port: Int = 8080,
    readTimeout: Int = 5000,
    errorStatus: Int => Status = defaultErrorStatus
  ): NanoHttpdServer[Effect] =
    val server = new NanoHttpdServer(handler, effectRunSync, port, readTimeout, errorStatus)
    server.start()
    server

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus = Map(
    ErrorType.ParseError -> Status.BAD_REQUEST,
    ErrorType.InvalidRequest -> Status.BAD_REQUEST,
    ErrorType.MethodNotFound -> Status.NOT_IMPLEMENTED,
    ErrorType.InvalidParams -> Status.BAD_REQUEST,
    ErrorType.InternalError -> Status.INTERNAL_ERROR,
    ErrorType.IOError -> Status.INTERNAL_ERROR,
    ErrorType.ApplicationError -> Status.INTERNAL_ERROR
  ).withDefaultValue(Status.INTERNAL_ERROR).map((errorType, status) => errorType.code -> status)
