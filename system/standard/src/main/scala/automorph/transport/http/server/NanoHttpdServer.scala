package automorph.transport.http.server

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.server.NanoHTTPD.Response.Status
import automorph.transport.http.server.NanoHTTPD.{IHTTPSession, Response, newFixedLengthResponse}
import automorph.transport.http.server.NanoHttpdServer.Context
import automorph.util.Extensions.ThrowableOps
import automorph.util.{Bytes, Network}
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.MapHasAsScala

/**
 * NanoHTTPD web server transport plugin using HTTP as message transport protocol.
 *
u* The server interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
 * @see [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
 * @constructor Creates a NanoHTTPD HTTP server with the specified RPC request ''handler''.
 * @param handler RPC request handler
 * @param evaluateEffect executes specified effect synchronously
 * @param port port to listen on for HTTP connections
 * @param readTimeout HTTP connection read timeout (milliseconds)
 * @param exceptionToStatusCode maps an exception to a corresponding default HTTP status code
 * @tparam Effect effect type
 */
final case class NanoHttpdServer[Effect[_]] private (
  handler: Handler.AnyCodec[Effect, Context],
  evaluateEffect: Effect[Response] => Response,
  port: Int,
  readTimeout: Int,
  exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
) extends NanoHTTPD(port) with Logging with ServerMessageTransport[Effect] {

  private val HeaderXForwardedFor = "X-Forwarded-For"
  private val system = handler.system

  override def close(): Effect[Unit] = system.impure(stop())

  override def start(): Unit = {
    logger.info("Listening for connections", Map("Port" -> port))
    super.start()
  }

  override def serve(session: IHTTPSession): Response = {
//    println(Bytes.string.to(Bytes.inputStream.from(session.getInputStream)))
    // Receive the request
    logger.trace("Receiving HTTP request", Map("Client" -> clientAddress(session)))
    val request = Bytes.inputStream.from(session.getInputStream)
    logger.debug("Received HTTP request", Map("Client" -> clientAddress(session), "Size" -> request.length))

    // Process the request
    implicit val usingContext: Context = createContext(session)
    evaluateEffect(system.map(
      system.either(handler.processRequest(request)),
      (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
        handlerResult.fold(
          error => serverError(error, request, session),
          result => {
            // Send the response
            val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
            val status = result.exception.map(exceptionToStatusCode).map(Status.lookup).getOrElse(Status.OK)
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
    val message = Bytes.string.from(error.trace.mkString("\n"))
    createResponse(message, status, session)
  }

  private def createResponse(message: ArraySeq.ofByte, status: Status, session: IHTTPSession): Response = {
    val client = clientAddress(session)
    logger.trace(
      "Sending HTTP response",
      Map("Client" -> client, "Status" -> status.getRequestStatus, "Size" -> message.length)
    )
    val inputStream = Bytes.inputStream.to(message)
    val response = newFixedLengthResponse(status, handler.codec.mediaType, inputStream, message.size.toLong)
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
}

case object NanoHttpdServer {

  /** Request context type. */
  type Context = Http[_]

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
   * @param exceptionToStatusCode maps an exception to a corresponding default HTTP status code
   * @tparam Effect effect type
   */
  def apply[Effect[_]](
    handler: Handler.AnyCodec[Effect, Context],
    runEffectSync: Effect[Response] => Response,
    port: Int,
    readTimeout: Int = 5000,
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
  ): NanoHttpdServer[Effect] = {
    val server = new NanoHttpdServer(handler, runEffectSync, port, readTimeout, exceptionToStatusCode)
    server.start()
    server
  }
}
