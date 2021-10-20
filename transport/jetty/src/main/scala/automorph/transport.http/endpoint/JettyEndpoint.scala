package automorph.transport.http.endpoint

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.endpoint.JettyEndpoint.Context
import automorph.util.Extensions.ThrowableOps
import automorph.util.{Bytes, Network, Random}
import jakarta.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import java.io.{ByteArrayInputStream, InputStream}
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
 * @param runEffect executes specified effect asynchronously
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class JettyEndpoint[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  runEffect: Effect[Any] => Unit,
  exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
) extends HttpServlet with Logging with EndpointMessageTransport {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system

  override def service(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    // Receive the request
    val requestId = Random.id
    lazy val requestDetails = requestProperties(request, requestId)
    logger.trace("Receiving HTTP request", requestDetails)
    val requestMessage: InputStream = request.getInputStream

    // Process the request
    implicit val usingContext: Context = createContext(request)
    runEffect(system.map(
      system.either(genericHandler.processRequest(requestMessage, requestId)),
      (handlerResult: Either[Throwable, HandlerResult[InputStream]]) =>
        handlerResult.fold(
          error => serverError(error, response, request, requestId, requestDetails),
          result => {
            // Send the response
            val message = result.response.getOrElse(new ByteArrayInputStream(Array()))
            val status = result.exception.map(exceptionToStatusCode).getOrElse(HttpStatus.OK_200)
            sendResponse(message, status, response, request, requestId)
          }
        )
    ))
  }

  private def serverError(
    error: Throwable,
    response: HttpServletResponse,
    request: HttpServletRequest,
    requestId: String,
    requestDetails: => Map[String, String]
  ): Unit = {
    logger.error("Failed to process HTTP request", error, requestDetails)
    val message = Bytes.inputStream.to(Bytes.string.from(error.trace.mkString("\n")))
    val status = HttpStatus.INTERNAL_SERVER_ERROR_500
    sendResponse(message, status, response, request, requestId)
  }

  private def sendResponse(
    message: InputStream,
    status: Int,
    response: HttpServletResponse,
    request: HttpServletRequest,
    requestId: String
  ): Unit = {
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "Status" -> status.toString
    )
    logger.debug("Sending HTTP response", responseDetails)
    response.setStatus(status)
    response.setContentType(genericHandler.protocol.codec.mediaType)
    val outputStream = response.getOutputStream
    IOUtils.copy(message, outputStream)
    outputStream.flush()
    logger.debug("Sent HTTP response", responseDetails)
  }

  private def createContext(request: HttpServletRequest): Context = {
    val headers = request.getHeaderNames.asScala.flatMap { name =>
      request.getHeaders(name).asScala.map(value => name -> value)
    }.toSeq
    Http(
      base = Some(request),
      method = Some(request.getMethod),
      headers = headers
    ).url(request.getRequestURI)
  }

  private def requestProperties(
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
  type Context = Http[HttpServletRequest]
}
