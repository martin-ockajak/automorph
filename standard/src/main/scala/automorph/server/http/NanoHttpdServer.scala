package automorph.server.http

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.protocol.{ErrorType, ResponseError}
import automorph.server.http.NanoHTTPD.Response.Status
import automorph.server.http.NanoHTTPD.{IHTTPSession, Response, newFixedLengthResponse}
import automorph.spi.Codec
import automorph.handler.Bytes
import scala.collection.immutable.ArraySeq

/**
 * HTTP server based on NanoHTTPD web server.
 *
 * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
 * @constructor Create an NanoHTTPD web server using the specified JSON-RPC request ''handler''.
 * @param handler JSON-RPC request handler
 * @param runEffectSync synchronous effect execution function
 * @param port port to listen on for HTTP connections
 * @param readTimeout HTTP connection read timeout (milliseconds)
 * @param errorStatus JSON-RPC error code to HTTP status mapping function
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 */
final case class NanoHttpdServer[Node, ExactCodec <: Codec[Node], Effect[_]] private (
  handler: Handler[Node, ExactCodec, Effect, IHTTPSession],
  runEffectSync: Effect[Response] => Response,
  port: Int,
  readTimeout: Int,
  errorStatus: Int => Status
) extends NanoHTTPD(port) with AutoCloseable with Logging {

  private val HeaderXForwardedFor = "X-Forwarded-For"
  private val backend = handler.backend

  override def start(): Unit = {
    logger.info("Listening for connections", Map("Port" -> port.toString))
    super.start()
  }

  override def serve(session: IHTTPSession): Response = {
    // Receive the request
    logger.trace("Receiving HTTP request", Map("Client" -> clientAddress(session)))
    val request = Bytes.inputStreamBytes.from(session.getInputStream)

    // Process the request
    implicit val usingContext = session
    runEffectSync(backend.map(
      backend.either(handler.processRequest(request)),
      (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) => handlerResult.fold(
        error => createServerError(error, session),
        result => {
          // Send the response
          val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
          val status = result.errorCode.map(errorStatus).getOrElse(Status.OK)
          createResponse(response, status, session)
        }
      )
    ))
  }

  private def createServerError(error: Throwable, session: IHTTPSession): Response = {
    val status = Status.INTERNAL_ERROR
    val message = Bytes.stringBytes.from(ResponseError.trace(error).mkString("\n"))
    logger.error("Failed to process HTTP request", error, Map("Client" -> clientAddress(session)))
    createResponse(message, status, session)
  }

  private def createResponse(message: ArraySeq.ofByte, status: Status, session: IHTTPSession): Response = {
    val client = clientAddress(session)
    logger.trace("Sending HTTP response", Map("Client" -> client, "Status" -> status.getRequestStatus.toString))
    val inputStream = Bytes.inputStreamBytes.to(message)
    val response = newFixedLengthResponse(status, handler.codec.mediaType, inputStream, message.size.toLong)
    logger.debug("Sent HTTP response", Map("Client" -> client, "Status" -> status.getRequestStatus.toString))
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

  /**
   * Create an NanoHTTPD web server using the specified JSON-RPC request ''handler''.
   *
   * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
   * @param handler JSON-RPC request handler
   * @param effectRunSync synchronous effect execution function
   * @param port port to listen on for HTTP connections
   * @param readTimeout HTTP connection read timeout (milliseconds)
   * @param errorStatus JSON-RPC error code to HTTP status mapping function
   * @tparam Node message format node representation type
   * @tparam ExactCodec message codec plugin type
   * @tparam Effect effect type
   */
  def apply[Node, ExactCodec <: Codec[Node], Effect[_]](
    handler: Handler[Node, ExactCodec, Effect, IHTTPSession],
    effectRunSync: Effect[Response] => Response,
    port: Int,
    readTimeout: Int = 5000,
    errorStatus: Int => Status = defaultErrorStatus
  ): NanoHttpdServer[Node, ExactCodec, Effect] = {
    val server = new NanoHttpdServer(handler, effectRunSync, port, readTimeout, errorStatus)
    server.start()
    server
  }

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus = Map(
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
