package automorph.transport.http.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.http.endpoint.JettyEndpoint.Context
import automorph.util.Extensions.{EffectOps, ThrowableOps}
import automorph.util.{Bytes, Network, Random}
import jakarta.servlet.AsyncContext
import jakarta.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import java.io.{ByteArrayInputStream, InputStream}
import java.net.URI
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.http.{HttpHeader, HttpStatus}
import scala.jdk.CollectionConverters.EnumerationHasAsScala

/**
 * Jetty HTTP endpoint message transport plugin.
 *
 * The servlet interprets HTTP request body as an RPC request and processes it with the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://www.eclipse.org/jetty Library documentation]]
 * @see [[https://www.eclipse.org/jetty/javadoc/jetty-11/index.html API]]
 * @constructor Creates a Jetty HTTP servlet with the specified RPC request handler.
 * @param handler RPC request handler
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class JettyEndpoint[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  exceptionToStatusCode: Throwable => Int = HttpContext.defaultExceptionToStatusCode
) extends HttpServlet with Logging with EndpointMessageTransport {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  implicit private val system: EffectSystem[Effect] = genericHandler.system

  override def service(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    // Log the request
    val asyncContext = request.startAsync()
    asyncContext.start(new Runnable {
      override def run(): Unit = {
        val requestId = Random.id
        lazy val requestProperties = extractRequestProperties(request, requestId)
        logger.trace("Receiving HTTP request", requestProperties)
        val requestBody: InputStream = request.getInputStream

        // Process the request
        implicit val givenContext: Context = requestContext(request)
        val path = new URI(request.getRequestURI).getPath
        genericHandler.processRequest(requestBody, requestId, Some(path)).either.map(_.fold(
          error => sendErrorResponse(error, response, asyncContext, request, requestId, requestProperties),
          result => {
            // Send the response
            val message = result.responseBody.getOrElse(new ByteArrayInputStream(Array()))
            val status = result.exception.map(exceptionToStatusCode).getOrElse(HttpStatus.OK_200)
            sendResponse(message, status, None, response, asyncContext, request, requestId)
          }
        )).run
      }
    })
  }

  private def sendErrorResponse(
    error: Throwable,
    response: HttpServletResponse,
    asyncContext: AsyncContext,
    request: HttpServletRequest,
    requestId: String,
    requestProperties: => Map[String, String]
  ): Unit = {
    logger.error("Failed to process HTTP request", error, requestProperties)
    val message = Bytes.inputStream.to(Bytes.string.from(error.trace.mkString("\n")))
    val status = HttpStatus.INTERNAL_SERVER_ERROR_500
    sendResponse(message, status, None, response, asyncContext, request, requestId)
  }

  private def sendResponse(
    message: InputStream,
    status: Int,
    responseContext: Option[Context],
    response: HttpServletResponse,
    asyncContext: AsyncContext,
    request: HttpServletRequest,
    requestId: String
  ): Unit = {
    // Log the response
    val responseStatus = responseContext.flatMap(_.statusCode).getOrElse(status)
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "Status" -> responseStatus.toString
    )
    logger.debug("Sending HTTP response", responseDetails)

    // Send the response
    response.setStatus(responseStatus)
    responseContext.toSeq.flatMap(_.headers).foreach { case (name, value) =>
      response.setHeader(name, value)
    }
    response.setContentType(genericHandler.protocol.codec.mediaType)
    val outputStream = response.getOutputStream
    IOUtils.copy(message, outputStream)
    outputStream.flush()
    asyncContext.complete()
    logger.debug("Sent HTTP response", responseDetails)
  }

  private def requestContext(request: HttpServletRequest): Context = {
    val headers = request.getHeaderNames.asScala.flatMap { name =>
      request.getHeaders(name).asScala.map(value => name -> value)
    }.toSeq
    HttpContext(
      base = Some(request),
      method = Some(request.getMethod),
      headers = headers
    ).url(request.getRequestURI)
  }

  private def extractRequestProperties(
    request: HttpServletRequest,
    requestId: String
  ): Map[String, String] = Map(
    LogProperties.requestId -> requestId,
    "Client" -> clientAddress(request),
    "URL" -> (request.getRequestURI + Option(request.getQueryString)
      .filter(_.nonEmpty).map("?" + _).getOrElse("")),
    "Method" -> request.getMethod
  )

  private def clientAddress(request: HttpServletRequest): String = {
    val forwardedFor = Option(request.getHeader(HttpHeader.X_FORWARDED_FOR.name))
    val address = request.getRemoteAddr
    Network.address(forwardedFor, address)
  }
}

object JettyEndpoint {

  /** Request context type. */
  type Context = HttpContext[HttpServletRequest]
}
