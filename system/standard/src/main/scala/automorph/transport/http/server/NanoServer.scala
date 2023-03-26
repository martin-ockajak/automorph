package automorph.transport.http.server

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, RequestHandler, ServerTransport}
import automorph.transport.http.server.NanoHTTPD.Response.Status
import automorph.transport.http.server.NanoHTTPD.{IHTTPSession, Response, newFixedLengthResponse}
import automorph.transport.http.server.NanoServer.Context
import automorph.transport.http.server.NanoWSD.WebSocketFrame.CloseCode
import automorph.transport.http.server.NanoWSD.{WebSocket, WebSocketFrame}
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, StringOps, ThrowableOps}
import automorph.util.{Network, Random}
import java.io.{IOException, InputStream}
import java.net.URI
import java.util.concurrent.{ArrayBlockingQueue, BlockingQueue}
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.MapHasAsScala

/**
 * NanoHTTPD HTTP & WebSocket server message transport plugin.
 *
 * Interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * - The response returned by the RPC request handler is used as HTTP response body.
 * - Processes only HTTP requests starting with specified URL path.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://github.com/NanoHttpd/nanohttpd Library documentation]]
 * @see
 *   [[https://javadoc.io/doc/org.nanohttpd/nanohttpd/latest/index.html API]]
 * @constructor
 *   Creates a NanoHTTPD HTTP & WebSocket server with specified effect system.
 * @param effectSystem
 *   effect system plugin
 * @param port
 *   port to listen on for HTTP connections
 * @param pathPrefix
 *   HTTP URL path prefix, only requests starting with this path prefix are allowed
 * @param methods
 *   allowed HTTP request methods
 * @param webSocket
 *   support upgrading of HTTP connections to use WebSocket protocol if true, support HTTP only if false
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @tparam Effect
 *   effect type
 */
final case class NanoServer[Effect[_]] (
  effectSystem: EffectSystem[Effect],
  port: Int,
  pathPrefix: String = "/",
  methods: Iterable[HttpMethod] = HttpMethod.values,
  webSocket: Boolean = true,
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
) extends NanoWSD(port) with Logging with ServerTransport[Effect, Context] {

  private var handler: RequestHandler[Effect, Context] = RequestHandler.dummy
  private val headerXForwardedFor = "X-Forwarded-For"
  private val log = MessageLog(logger, Protocol.Http.name)
  private val allowedMethods = methods.map(_.name).toSet
  implicit private val system: EffectSystem[Effect] = effectSystem

  override def clone(rpcHandler: RequestHandler[Effect, Context]): NanoServer[Effect] = {
    this.handler = rpcHandler
    this
  }

  override def init(): Effect[Unit] =
    system.evaluate {
      super.start()
      (Seq(Protocol.Http) ++ Option.when(webSocket)(Protocol.WebSocket)).foreach { protocol =>
        logger.info("Listening for connections", ListMap("Protocol" -> protocol, "Port" -> port.toString))
      }
    }

  override def close(): Effect[Unit] =
    system.evaluate(stop())

  /**
   * Serve HTTP session.
   *
   * @param session
   *   HTTP session
   * @return
   *   HTTP response
   */
  override protected def serveHttp(session: IHTTPSession): BlockingQueue[Response] = {
    // Validate URL path
    val queue = new ArrayBlockingQueue[Response](1)
    val url = new URI(session.getUri)
    if (!url.getPath.startsWith(pathPrefix)) {
      queue.add(newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found"))
    } else {
      // Validate HTTP request method
      if (!allowedMethods.contains(session.getMethod.toString.toUpperCase)) {
        queue.add(
          newFixedLengthResponse(Status.METHOD_NOT_ALLOWED, NanoHTTPD.MIME_PLAINTEXT, "Method Not Allowed")
        )
      } else {
        // Receive the request
        val protocol = Protocol.Http
        val requestId = Random.id
        lazy val requestProperties = getRequestProperties(session, protocol, requestId)
        log.receivingRequest(requestProperties, Protocol.Http.name)
        val requestBody = session.getInputStream.asArray(session.getBodySize.toInt).toInputStream

        // Handle the request
        handleRequest(requestBody, session, protocol, requestProperties, requestId).map { response =>
          queue.add(response)
        }.runAsync
      }
    }
    queue
  }

  /**
   * Serve WebSocket handshake session.
   *
   * @param session
   *   WebSocket handshake session
   * @return
   *   WebSocket handler
   */
  override protected def openWebSocket(session: IHTTPSession): WebSocket =
    new WebSocket(session) {

      override protected def onOpen(): Unit =
        if (!webSocket) { this.close(CloseCode.PolicyViolation, "WebSocket support disabled", true) }

      override protected def onClose(code: WebSocketFrame.CloseCode, reason: String, initiatedByRemote: Boolean): Unit =
        ()

      protected def onMessage(frame: WebSocketFrame): Unit = {
        // Receive the request
        val protocol = Protocol.WebSocket
        val requestId = Random.id
        lazy val requestProperties = getRequestProperties(session, protocol, requestId)
        val request = frame.getBinaryPayload.toInputStream
        handleRequest(request, session, protocol, requestProperties, requestId).map { response =>
          // Send the response
          send(response.getData.toArray)
        }.runAsync
      }

      override protected def onPong(pong: WebSocketFrame): Unit =
        ()

      override protected def onException(error: IOException): Unit =
        log.failedReceiveRequest(error, Map(), Protocol.WebSocket.name)
    }

  private def handleRequest(
    requestBody: InputStream,
    session: IHTTPSession,
    protocol: Protocol,
    requestProperties: => Map[String, String],
    requestId: String,
  ): Effect[Response] = {
    log.receivedRequest(requestProperties, protocol.name)

    // Process the request
    handler.processRequest(requestBody, getRequestContext(session), requestId).either.map(
      _.fold(
        error => sendErrorResponse(error, session, protocol, requestId, requestProperties),
        result => {
          // Send the response
          val responseBody = result.map(_.responseBody).getOrElse(Array[Byte]().toInputStream)
          val status = result.flatMap(_.exception).map(mapException).map(Status.lookup).getOrElse(Status.OK)
          createResponse(responseBody, status, result.flatMap(_.context), session, protocol, requestId)
        },
      )
    )
  }

  private def sendErrorResponse(
    error: Throwable,
    session: IHTTPSession,
    protocol: Protocol,
    requestId: String,
    requestProperties: => Map[String, String],
  ) = {
    log.failedProcessRequest(error, requestProperties, protocol.name)
    val message = error.description.toInputStream
    createResponse(message, Status.INTERNAL_ERROR, None, session, protocol, requestId)
  }

  private def createResponse(
    responseBody: InputStream,
    status: Status,
    responseContext: Option[Context],
    session: IHTTPSession,
    protocol: Protocol,
    requestId: String,
  ): Response = {
    // Log the response
    val responseStatus = responseContext.flatMap(_.statusCode.map(Status.lookup)).getOrElse(status)
    lazy val responseProperties = Map(LogProperties.requestId -> requestId, "Client" -> clientAddress(session)) ++
      (protocol match {
        case Protocol.Http => Some("Status" -> responseStatus.toString)
        case _ => None
      })
    log.sendingResponse(responseProperties, protocol.name)

    // Create the response
    val responseData = responseBody.toArray
    val response = newFixedLengthResponse(
      responseStatus,
      handler.mediaType,
      responseData.toInputStream,
      responseData.length.toLong
    )
    setResponseContext(response, responseContext)
    log.sentResponse(responseProperties, protocol.name)
    response
  }

  private def setResponseContext(response: Response, responseContext: Option[Context]): Unit =
    responseContext.toSeq.flatMap(_.headers).foreach { case (name, value) => response.addHeader(name, value) }

  private def getRequestContext(session: IHTTPSession): Context = {
    val http = HttpContext(
      message = Some(session),
      method = Some(HttpMethod.valueOf(session.getMethod.name)),
      headers = session.getHeaders.asScala.toSeq,
    ).url(session.getUri).scheme("http").host("localhost").port(port)
    Option(session.getQueryParameterString).map(http.query).getOrElse(http)
  }

  private def getRequestProperties(
    session: IHTTPSession,
    protocol: Protocol,
    requestId: String,
  ): Map[String, String] = {
    val query = Option(session.getQueryParameterString).filter(_.nonEmpty).map("?" + _).getOrElse("")
    val url = s"${session.getUri}$query"
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(session),
      "Protocol" -> protocol.toString,
      "URL" -> url,
    ) ++ Option.when(protocol == Protocol.Http)("Method" -> session.getMethod.toString)
  }

  private def clientAddress(session: IHTTPSession): String = {
    val forwardedFor = Option(session.getHeaders.get(headerXForwardedFor))
    val address = session.getRemoteHostName
    Network.address(forwardedFor, address)
  }
}

object NanoServer {

  /** Request context type. */
  type Context = HttpContext[IHTTPSession]

  /** Response type. */
  type Response = NanoHTTPD.Response
}
