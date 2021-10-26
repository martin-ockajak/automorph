package automorph.transport.http.server

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.ServerMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.server.NanoHTTPD
import automorph.transport.http.server.NanoHTTPD.Response.Status
import automorph.transport.http.server.NanoHTTPD.{IHTTPSession, Response, newFixedLengthResponse}
import automorph.transport.http.server.NanoServer.{Context, Protocol}
import automorph.transport.http.server.NanoWSD.{WebSocket, WebSocketFrame}
import automorph.transport.http.server.NanoWSD.WebSocketFrame.CloseCode
import automorph.util.Extensions.ThrowableOps
import automorph.util.{Bytes, Network, Random}
import java.io.IOException
import java.net.URI
import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.MapHasAsScala

/**
 * NanoHTTPD HTTP server transport plugin.
 *
 * The server interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://github.com/NanoHttpd/nanohttpd Library documentation]]
 * @see [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
 * @constructor Creates a NanoHTTPD HTTP & WebSocket server with the specified RPC request handler.
 * @param handler RPC request handler
 * @param executeEffect executes specified effect synchronously
 * @param port port to listen on for HTTP connections
 * @param path HTTP URL path (default: /)
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @param webSocket support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
 * @tparam Effect effect type
 */
final case class NanoServer[Effect[_]] private (
  handler: Types.HandlerAnyCodec[Effect, Context],
  executeEffect: Effect[Response] => Response,
  port: Int,
  path: String = "/",
  exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
  webSocket: Boolean = true
) extends NanoWSD(port) with Logging with ServerMessageTransport[Effect] {

  private val HeaderXForwardedFor = "X-Forwarded-For"
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system

  override def close(): Effect[Unit] = system.wrap(stop())

  override def start(): Unit = {
    logger.info("Listening for connections", Map("Port" -> port))
    super.start()
  }

  override protected def serveHttp(session: IHTTPSession): Response = {
    // Validate URL path
    val url = new URI(session.getUri)
    if (!url.getPath.startsWith(path)) {
      newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found")
    } else {
      // Receive the request
      val protocol = Protocol.Http
      val requestId = Random.id
      lazy val requestDetails = requestProperties(session, protocol, requestId)
      logger.trace("Receiving HTTP request", requestDetails)
      val request = Bytes.inputStream.from(session.getInputStream, session.getBodySize.toInt)

      // Handler the request
      handleRequest(request, session, protocol, Some(url.getPath), requestDetails, requestId)
    }
  }

  override protected def openWebSocket(session: IHTTPSession) = new WebSocket(session) {

    override protected def onOpen(): Unit =
      if (!webSocket) {
        this.close(CloseCode.PolicyViolation, "WebSocket support disabled", true)
      }

    override protected def onClose(code: WebSocketFrame.CloseCode, reason: String, initiatedByRemote: Boolean): Unit = ()

    protected def onMessage(frame: WebSocketFrame): Unit = {
      // Receive the request
      val protocol = Protocol.WebSocket
      val requestId = Random.id
      lazy val requestDetails = requestProperties(session, protocol, requestId)
      val request = Bytes.byteArray.from(frame.getBinaryPayload)
      val response = handleRequest(request, session, protocol, None, requestDetails, requestId)

      // Handler the request
      send(Bytes.byteArray.to(Bytes.inputStream.from(response.getData)))
    }

    override protected def onPong(pong: WebSocketFrame): Unit = ()

    override protected def onException(exception: IOException): Unit =
      logger.error(s"Failed to receive ${Protocol.WebSocket} request", exception)
  }

  private def handleRequest(
    request: ArraySeq.ofByte,
    session: IHTTPSession,
    protocol: Protocol,
    functionName: Option[String],
    requestDetails: Map[String, String],
    requestId: String
  ): Response = {
    logger.debug(s"Received $protocol request", requestDetails)

    // Process the request
    implicit val usingContext: Context = requestContext(session)
    executeEffect(system.map(
      system.either(genericHandler.processRequest(request, requestId, functionName)),
      (handlerResult: Either[Throwable, HandlerResult[ArraySeq.ofByte, Context]]) =>
        handlerResult.fold(
          error => serverError(error, session, protocol, requestId, requestDetails),
          result => {
            // Send the response
            val response = result.responseBody.getOrElse(new ArraySeq.ofByte(Array()))
            val status = result.exception.map(exceptionToStatusCode).map(Status.lookup).getOrElse(Status.OK)
            createResponse(response, status, result.context, session, protocol, requestId)
          }
        )
    ))
  }

  private def serverError(
    error: Throwable,
    session: IHTTPSession,
    protocol: Protocol,
    requestId: String,
    requestDetails: => Map[String, String]
  ) = {
    logger.error(s"Failed to process $protocol request", error, requestDetails)
    val message = Bytes.string.from(error.trace.mkString("\n"))
    createResponse(message, Status.INTERNAL_ERROR, None, session, protocol, requestId)
  }

  private def createResponse(
    message: ArraySeq.ofByte,
    status: Status,
    responseContext: Option[Context],
    session: IHTTPSession,
    protocol: Protocol,
    requestId: String
  ): Response = {
    val responseStatus = responseContext.flatMap(_.statusCode.map(Status.lookup)).getOrElse(status)
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(session)
    ) ++ (protocol match {
      case Protocol.Http => Some("Status" -> responseStatus.toString)
      case _ => None
    })
    logger.trace(s"Sending $protocol response", responseDetails)
    val inputStream = Bytes.inputStream.to(message)
    val mediaType = genericHandler.protocol.codec.mediaType
    val response = newFixedLengthResponse(responseStatus, mediaType, inputStream, message.size.toLong)
    responseContext.toSeq.flatMap(_.headers).foreach { case (name, value) =>
      response.addHeader(name, value)
    }
    response.setMimeType(mediaType)
    logger.debug(s"Sent $protocol response", responseDetails)
    response
  }

  private def requestContext(session: IHTTPSession): Context = {
    val http = Http(
      base = Some(session),
      method = Some(session.getMethod.name),
      headers = session.getHeaders.asScala.toSeq
    ).url(session.getUri).scheme("http").host("localhost").port(port)
    Option(session.getQueryParameterString).map(http.query).getOrElse(http)
  }

  private def requestProperties(
    session: IHTTPSession,
    protocol: Protocol,
    requestId: String
  ): Map[String, String] = Map(
    LogProperties.requestId -> requestId,
    "Client" -> clientAddress(session),
    "URL" -> (session.getUri + Option(session.getQueryParameterString)
      .filter(_.nonEmpty).map("?" + _).getOrElse(""))
  ) ++ (protocol match {
    case Protocol.Http => Some("Method" -> session.getMethod.toString)
    case _ => None
  })

  private def clientAddress(session: IHTTPSession): String = {
    val forwardedFor = Option(session.getHeaders.get(HeaderXForwardedFor))
    val address = session.getRemoteHostName
    Network.address(forwardedFor, address)
  }
}

object NanoServer {

  /** Request context type. */
  type Context = Http[_]

  /** Response type. */
  type Response = NanoHTTPD.Response

  /**
   * Creates a NanoHTTPD web server with the specified RPC request handler.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://github.com/NanoHttpd/nanohttpd Library documentation]]
   * @see [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
   * @param handler RPC request handler
   * @param runEffectSync synchronous effect execution function
   * @param port port to listen on for HTTP connections
   * @param path HTTP URL path (default: /)
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @param webSocket support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
   * @tparam Effect effect type
   */
  def apply[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    runEffectSync: Effect[Response] => Response,
    port: Int,
    path: String = "/",
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode,
    webSocket: Boolean = true
  ): NanoServer[Effect] = {
    val server = new NanoServer(handler, runEffectSync, port, path, exceptionToStatusCode)
    server.start()
    server
  }

  /** Transport protocol. */
  private sealed abstract class Protocol(val name: String) {
    override def toString: String = name
  }

  /** Transport protocols. */
  private object Protocol {

    case object Http extends Protocol("HTTP")

    case object WebSocket extends Protocol("WebSocket")
  }
}