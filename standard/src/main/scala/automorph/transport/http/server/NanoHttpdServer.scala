package automorph.transport.http.server

import automorph.Handler
import automorph.handler.{Bytes, HandlerResult}
import automorph.log.Logging
import automorph.protocol.{ErrorType, ResponseError}
import automorph.spi.ServerMessageTransport
import automorph.transport.http.server.NanoHTTPD.Response.Status
import automorph.transport.http.server.NanoHTTPD.{newFixedLengthResponse, IHTTPSession, Response}
import scala.collection.immutable.ArraySeq

/**
 * NanoHTTPD web server server transport using HTTP as message transport protocol.
 *
 * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
 * @see [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
 * @constructor Creates an NanoHTTPD web server using the specified JSON-RPC request ''handler''.
 * @param handler JSON-RPC request handler
 * @param runEffectSync synchronous effect execution function
 * @param port port to listen on for HTTP connections
 * @param readTimeout HTTP connection read timeout (milliseconds)
 * @param errorStatus JSON-RPC error code to HTTP status mapping function
 * @tparam Effect effect type
 */
final case class NanoHttpdServer[Effect[_]] private (
  handler: Handler.AnyFormat[Effect, IHTTPSession],
  runEffectSync: Effect[Response] => Response,
  port: Int,
  readTimeout: Int,
  errorStatus: Int => Status
) extends NanoHTTPD(port) with Logging with ServerMessageTransport {

  private val HeaderXForwardedFor = "X-Forwarded-For"
  private val system = handler.system

  override def start(): Unit = {
    logger.info("Listening for connections", Map("Port" -> port))
    super.start()
  }

  override def serve(session: IHTTPSession): Response = {
    // Receive the request
    logger.trace("Receiving HTTP request", Map("Client" -> clientAddress(session)))
    val request = Bytes.inputStreamBytes.from(session.getInputStream)
    logger.debug("Received HTTP request", Map("Client" -> clientAddress(session), "Size" -> request.length))

    // Process the request
    implicit val usingContext: IHTTPSession = session
    runEffectSync(system.map(
      system.either(handler.processRequest(request)),
      (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
        handlerResult.fold(
          error => serverError(error, request, session),
          result => {
            // Send the response
            val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
            val status = result.errorCode.map(errorStatus).getOrElse(Status.OK)
            createResponse(response, status, session)
          }
        )
    ))
  }

  private def serverError(error: Throwable, request: ArraySeq.ofByte, session: IHTTPSession): Response = {
    val status = Status.INTERNAL_ERROR
    val message = Bytes.stringBytes.from(ResponseError.trace(error).mkString("\n"))
    logger.error(
      "Failed to process HTTP request",
      error,
      Map("Client" -> clientAddress(session), "Size" -> request.length)
    )
    createResponse(message, status, session)
  }

  private def createResponse(message: ArraySeq.ofByte, status: Status, session: IHTTPSession): Response = {
    val client = clientAddress(session)
    logger.trace(
      "Sending HTTP response",
      Map("Client" -> client, "Status" -> status.getRequestStatus, "Size" -> message.length)
    )
    val inputStream = Bytes.inputStreamBytes.to(message)
    val response = newFixedLengthResponse(status, handler.format.mediaType, inputStream, message.size.toLong)
    logger.debug(
      "Sent HTTP response",
      Map("Client" -> client, "Status" -> status.getRequestStatus, "Size" -> message.length)
    )
    response
  }

  private def clientAddress(session: IHTTPSession): String = {
    val forwardedFor = Option(session.getHeaders.get(HeaderXForwardedFor))
    forwardedFor.map(_.split(",", 2)(0)).getOrElse {
      session.getRemoteHostName
    }
  }

  override def close(): Unit = stop()
}

case object NanoHttpdServer {

  /** Request context type. */
  type Context = IHTTPSession

  /** Request type. */
  type Response = automorph.transport.http.server.NanoHTTPD.Response

  /**
   * Create an NanoHTTPD web server using the specified JSON-RPC request ''handler''.
   *
   * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
   * @param handler JSON-RPC request handler
   * @param runEffectSync synchronous effect execution function
   * @param port port to listen on for HTTP connections
   * @param readTimeout HTTP connection read timeout (milliseconds)
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @tparam Effect effect type
   */
  def apply[Effect[_]](
    handler: Handler.AnyFormat[Effect, IHTTPSession],
    runEffectSync: Effect[Response] => Response,
    port: Int,
    readTimeout: Int = 5000,
    errorStatus: Int => Status = defaultErrorStatus
  ): NanoHttpdServer[Effect] = {
    val server = new NanoHttpdServer(handler, runEffectSync, port, readTimeout, errorStatus)
    server.start()
    server
  }

  /** Error propagating mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus: Int => Status = Map(
    ErrorType.ParseError -> Status.BAD_REQUEST,
    ErrorType.InvalidRequest -> Status.BAD_REQUEST,
    ErrorType.MethodNotFound -> Status.NOT_IMPLEMENTED,
    ErrorType.InvalidParams -> Status.BAD_REQUEST,
    ErrorType.InternalError -> Status.INTERNAL_ERROR,
    ErrorType.IOError -> Status.INTERNAL_ERROR,
    ErrorType.ApplicationError -> Status.INTERNAL_ERROR
  ).withDefaultValue(Status.INTERNAL_ERROR).map { case (errorType, status) =>
    errorType.code -> status
  }
}
