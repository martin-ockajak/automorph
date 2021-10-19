package automorph.transport.http.server

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.server.NanoHTTPD.Response.Status
import automorph.transport.http.server.NanoHTTPD.{IHTTPSession, Response, newFixedLengthResponse}
import automorph.transport.http.server.NanoHttpdServer.Context
import automorph.util.Extensions.ThrowableOps
import automorph.util.{Bytes, Network, Random}
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.MapHasAsScala

/**
 * NanoHTTPD web server HTTP server transport plugin.
 *
 * The server interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://github.com/NanoHttpd/nanohttpd Library documentation]]
 * @see [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
 * @constructor Creates a NanoHTTPD HTTP server with the specified RPC request handler.
 * @param handler RPC request handler
 * @param evaluateEffect executes specified effect synchronously
 * @param port port to listen on for HTTP connections
 * @param readTimeout HTTP connection read timeout (milliseconds)
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class NanoHttpdServer[Effect[_]] private (
  handler: Types.HandlerAnyCodec[Effect, Context],
  evaluateEffect: Effect[Response] => Response,
  port: Int,
  readTimeout: Int,
  exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
) extends NanoHTTPD(port) with Logging with ServerMessageTransport[Effect] {

  private val HeaderXForwardedFor = "X-Forwarded-For"
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system

  override def close(): Effect[Unit] = system.wrap(stop())

  override def start(): Unit = {
    logger.info("Listening for connections", Map("Port" -> port))
    super.start()
  }

  override def serve(session: IHTTPSession): Response = {
    // Receive the request
    val requestId = Random.id
    lazy val requestDetails = requestProperties(session, requestId)
    logger.trace("Receiving HTTP request", requestDetails)
    val request = Bytes.inputStream.from(session.getInputStream, session.getBodySize.toInt)
    logger.debug("Received HTTP request", requestDetails)

    // Process the request
    implicit val usingContext: Context = createContext(session)
    evaluateEffect(system.map(
      system.either(genericHandler.processRequest(request, requestId)),
      (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte]]) =>
        handlerResult.fold(
          error => serverError(error, session, requestId, requestDetails),
          result => {
            // Send the response
            val response = result.response.getOrElse(new ArraySeq.ofByte(Array()))
            val status = result.exception.map(exceptionToStatusCode).map(Status.lookup).getOrElse(Status.OK)
            createResponse(response, status, session, requestId)
          }
        )
    ))
  }

  private def serverError(
    error: Throwable,
    session: IHTTPSession,
    requestId: String,
    requestDetails: => Map[String, String]
  ): Response = {
    logger.error("Failed to process HTTP request", error, requestDetails)
    val status = Status.INTERNAL_ERROR
    val message = Bytes.string.from(error.trace.mkString("\n"))
    createResponse(message, status, session, requestId)
  }

  private def createResponse(
    message: ArraySeq.ofByte,
    status: Status,
    session: IHTTPSession,
    requestId: String
  ): Response = {
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(session),
      "Status" -> status.toString
    )
    logger.trace("Sending HTTP response", responseDetails)
    val inputStream = Bytes.inputStream.to(message)
    val mediaType = genericHandler.protocol.codec.mediaType
    val response = newFixedLengthResponse(status, mediaType, inputStream, message.size.toLong)
    logger.debug("Sent HTTP response", responseDetails)
    response
  }

  private def createContext(session: IHTTPSession): Context = {
    val http = Http(
      base = Some(session),
      method = Some(session.getMethod.name),
      headers = session.getHeaders.asScala.toSeq
    ).url(session.getUri).scheme("http").host("localhost").port(port)
    Option(session.getQueryParameterString).map(http.query).getOrElse(http)
  }

  private def requestProperties(
    session: IHTTPSession,
    requestId: String
  ): Map[String, String] = Map(
    LogProperties.requestId -> requestId,
    "Client" -> clientAddress(session),
    "URL" -> (session.getUri + Option(session.getQueryParameterString)
      .filter(_.nonEmpty).map("?" + _).getOrElse("")),
    "Method" -> session.getMethod.toString
  )

  private def clientAddress(session: IHTTPSession): String = {
    val forwardedFor = Option(session.getHeaders.get(HeaderXForwardedFor))
    val address = session.getRemoteHostName
    Network.address(forwardedFor, address)
  }
}

object NanoHttpdServer {

  /** Request context type. */
  type Context = Http[_]

  /** Response type. */
  type Response = automorph.transport.http.server.NanoHTTPD.Response

  /**
   * Creates a NanoHTTPD web server with the specified RPC request handler.
   *
   * @see [[https://github.com/NanoHttpd/nanohttpd Documentation]]
   * @see [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
   * @param handler RPC request handler
   * @param runEffectSync synchronous effect execution function
   * @param port port to listen on for HTTP connections
   * @param readTimeout HTTP connection read timeout (milliseconds)
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @tparam Effect effect type
   */
  def apply[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
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
