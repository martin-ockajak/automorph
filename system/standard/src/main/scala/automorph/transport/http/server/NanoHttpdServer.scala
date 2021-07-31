package automorph.transport.http.server

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.protocol.jsonrpc.ResponseError
import automorph.spi.ServerMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.Http
import automorph.transport.http.server.NanoHTTPD.Response.Status
import automorph.transport.http.server.NanoHTTPD.{IHTTPSession, Response, newFixedLengthResponse}
import automorph.transport.http.server.NanoHttpdServer.Context
import automorph.util.{Bytes, Network}
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.MapHasAsScala

/**
 * NanoHTTPD web server transport plugin using HTTP as message transport protocol.
 *
 * The server interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
 * @see [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
 * @constructor Creates a NanoHTTPD HTTP server with the specified RPC request ''handler''.
 * @param handler RPC request handler
 * @param runEffectSync synchronous effect execution function
 * @param port port to listen on for HTTP connections
 * @param readTimeout HTTP connection read timeout (milliseconds)
 * @param errorStatusCode maps a JSON-RPC error to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class NanoHttpdServer[Effect[_]] private (
  handler: Handler.AnyFormat[Effect, Context],
  runEffectSync: Effect[Response] => Response,
  port: Int,
  readTimeout: Int,
  errorStatusCode: Int => Int = Http.defaultErrorStatusCode
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
    val request = Bytes.inputStream.from(session.getInputStream)
    logger.debug("Received HTTP request", Map("Client" -> clientAddress(session), "Size" -> request.length))

    // Process the request
    implicit val usingContext: Context = createContext(session)
    runEffectSync(system.map(
      system.either(handler.processRequest(request)),
      (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
        handlerResult.fold(
          error => serverError(error, request, session),
          result => {
            // Send the response
            val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
            val status = result.errorCode.map(errorStatusCode).map(Status.lookup).getOrElse(Status.OK)
            createResponse(response, status, session)
          }
        )
    ))
  }

  private def serverError(error: Throwable, request: ArraySeq.ofByte, session: IHTTPSession): Response = {
    logger.error(
      "Failed to process HTTP request",
      error,
      Map("Client" -> clientAddress(session), "Size" -> request.length)
    )
    val status = Status.INTERNAL_ERROR
    val message = Bytes.string.from(ResponseError.trace(error).mkString("\n"))
    createResponse(message, status, session)
  }

  private def createResponse(message: ArraySeq.ofByte, status: Status, session: IHTTPSession): Response = {
    val client = clientAddress(session)
    logger.trace(
      "Sending HTTP response",
      Map("Client" -> client, "Status" -> status.getRequestStatus, "Size" -> message.length)
    )
    val inputStream = Bytes.inputStream.to(message)
    val response = newFixedLengthResponse(status, handler.format.mediaType, inputStream, message.size.toLong)
    logger.debug(
      "Sent HTTP response",
      Map("Client" -> client, "Status" -> status.getRequestStatus, "Size" -> message.length)
    )
    response
  }

  private def createContext(session: IHTTPSession): Context = {
    Http(
      source = Some(session),
      method = Some(session.getMethod.name),
      headers = session.getHeaders.asScala.toSeq
    ).url(session.getUri)
  }

  private def clientAddress(session: IHTTPSession): String = {
    val forwardedFor = Option(session.getHeaders.get(HeaderXForwardedFor))
    val address = session.getRemoteHostName
    Network.address(forwardedFor, address)
  }

  override def close(): Unit = stop()
}

case object NanoHttpdServer {

  /** Request context type. */
  type Context = Http[IHTTPSession]

  /** Request type. */
  type Response = automorph.transport.http.server.NanoHTTPD.Response

  /**
   * Creates a NanoHTTPD web server with the specified RPC request ''handler''.
   *
   * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
   * @see [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
   * @param handler RPC request handler
   * @param runEffectSync synchronous effect execution function
   * @param port port to listen on for HTTP connections
   * @param readTimeout HTTP connection read timeout (milliseconds)
   * @param errorStatusCode maps a JSON-RPC error to a corresponding HTTP status code
   * @tparam Effect effect type
   */
  def apply[Effect[_]](
    handler: Handler.AnyFormat[Effect, Context],
    runEffectSync: Effect[Response] => Response,
    port: Int,
    readTimeout: Int = 5000,
    errorStatusCode: Int => Int = Http.defaultErrorStatusCode
  ): NanoHttpdServer[Effect] = {
    val server = new NanoHttpdServer(handler, runEffectSync, port, readTimeout, errorStatusCode)
    server.start()
    server
  }
}
