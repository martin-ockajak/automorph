package automorph.transport.http.endpoint

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}
import automorph.transport.http.endpoint.JettyHttpEndpoint.Context
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, StringOps, ThrowableOps, TryOps}
import automorph.util.{Network, Random}
import jakarta.servlet.AsyncContext
import jakarta.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import java.io.InputStream
import org.eclipse.jetty.http.{HttpHeader, HttpStatus}
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.EnumerationHasAsScala
import scala.util.Try

/**
 * Jetty HTTP endpoint message transport plugin.
 *
 * Interprets HTTP request body as an RPC request and processes it with the specified RPC handler.
 *   - The response returned by the RPC handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://www.eclipse.org/jetty Library documentation]]
 * @see
 *   [[https://www.eclipse.org/jetty/javadoc/jetty-11/index.html API]]
 * @constructor
 *   Creates an Jetty HTTP endpoint message transport plugin with specified effect system and request handler.
 * @param effectSystem
 *   effect system plugin
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @param handler
 *   RPC request handler
 * @tparam Effect
 *   effect type
 */
final case class JettyHttpEndpoint[Effect[_]](
  effectSystem: EffectSystem[Effect],
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy[Effect, Context],
) extends HttpServlet with Logging with EndpointTransport[Effect, Context, HttpServlet] {

  private val log = MessageLog(logger, Protocol.Http.name)
  implicit private val system: EffectSystem[Effect] = effectSystem

  override def adapter: HttpServlet =
    this

  override def clone(handler: RequestHandler[Effect, Context]): JettyHttpEndpoint[Effect] =
    copy(handler = handler)

  override def service(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    // Log the request
    val asyncContext = request.startAsync()
    asyncContext.start { () =>
      val requestId = Random.id
      lazy val requestProperties = getRequestProperties(request, requestId)
      log.receivedRequest(requestProperties)
      val requestBody = request.getInputStream

      // Process the request
      handler.processRequest(requestBody, getRequestContext(request), requestId).either.map(
        _.fold(
          error => sendErrorResponse(error, response, asyncContext, request, requestId, requestProperties),
          result => {
            // Send the response
            val responseBody = result.map(_.responseBody).getOrElse(Array[Byte]().toInputStream)
            val status = result.flatMap(_.exception).map(mapException).getOrElse(HttpStatus.OK_200)
            sendResponse(responseBody, status, result.flatMap(_.context), response, asyncContext, request, requestId)
          },
        )
      ).runAsync
    }
  }

  private def sendErrorResponse(
    error: Throwable,
    response: HttpServletResponse,
    asyncContext: AsyncContext,
    request: HttpServletRequest,
    requestId: String,
    requestProperties: => Map[String, String],
  ): Unit = {
    log.failedProcessRequest(error, requestProperties)
    val responseBody = error.description.toInputStream
    val status = HttpStatus.INTERNAL_SERVER_ERROR_500
    sendResponse(responseBody, status, None, response, asyncContext, request, requestId)
  }

  private def sendResponse(
    responseBody: InputStream,
    status: Int,
    responseContext: Option[Context],
    response: HttpServletResponse,
    asyncContext: AsyncContext,
    request: HttpServletRequest,
    requestId: String,
  ): Unit = {
    // Log the response
    val responseStatus = responseContext.flatMap(_.statusCode).getOrElse(status)
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "Status" -> responseStatus.toString,
    )
    log.sendingResponse(responseProperties)

    // Send the response
    Try {
      setResponseContext(response, responseContext)
      response.setContentType(handler.mediaType)
      response.setStatus(responseStatus)
      val outputStream = response.getOutputStream
      responseBody.transferTo(outputStream)
      outputStream.flush()
      asyncContext.complete()
      log.sentResponse(responseProperties)
    }.onFailure(error => log.failedSendResponse(error, responseProperties)).get
  }

  private def setResponseContext(response: HttpServletResponse, responseContext: Option[Context]): Unit =
    responseContext.toSeq.flatMap(_.headers).foreach { case (name, value) => response.setHeader(name, value) }

  private def getRequestContext(request: HttpServletRequest): Context = {
    val headers = request.getHeaderNames.asScala.flatMap { name =>
      request.getHeaders(name).asScala.map(value => name -> value)
    }.toSeq
    HttpContext(message = Some(request), method = Some(HttpMethod.valueOf(request.getMethod)), headers = headers)
      .url(request.getRequestURI)
  }

  private def getRequestProperties(request: HttpServletRequest, requestId: String): Map[String, String] = {
    val query = Option(request.getQueryString).filter(_.nonEmpty).map("?" + _).getOrElse("")
    val url = s"${request.getRequestURI}$query"
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "URL" -> url,
      "Method" -> request.getMethod,
    )
  }

  private def clientAddress(request: HttpServletRequest): String = {
    val forwardedFor = Option(request.getHeader(HttpHeader.X_FORWARDED_FOR.name))
    val address = request.getRemoteAddr
    Network.address(forwardedFor, address)
  }
}

object JettyHttpEndpoint {

  /** Request context type. */
  type Context = HttpContext[HttpServletRequest]
}
