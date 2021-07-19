package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.protocol.{ErrorType, ResponseError}
import automorph.spi.EndpointMessageTransport
import automorph.transport.http.HttpProperties
import automorph.transport.http.endpoint.JettyEndpoint.{Context, defaultErrorStatus}
import automorph.util.{Bytes, Network}
import jakarta.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import java.io.{ByteArrayInputStream, InputStream}
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.http.{HttpHeader, HttpStatus}
import scala.jdk.CollectionConverters.EnumerationHasAsScala

/**
 * Jetty web server endpoint transport plugin using HTTP as message transport protocol.
 *
 * The servlet interprets HTTP request body as an RPC request and processes it with the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://www.eclipse.org/jetty/ Documentation]]
 * @see [[https://www.eclipse.org/jetty/javadoc/jetty-11/index.html API]]
 * @constructor Creates a Jetty web server HTTP servlet with the specified RPC request ''handler''.
 * @param handler RPC request handler
 * @param runEffect asynchronous effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status code mapping function
 * @tparam Effect effect type
 */
final case class JettyEndpoint[Effect[_]](
  handler: Handler.AnyFormat[Effect, Context],
  runEffect: Effect[Any] => Any,
  errorStatus: Int => Int = defaultErrorStatus
) extends HttpServlet with Logging with EndpointMessageTransport {

  private val system = handler.system

  override def service(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    // Receive the request
    val client = clientAddress(request)
    logger.trace("Receiving HTTP request", Map("Client" -> client))
    val requestMessage: InputStream = request.getInputStream

    // Process the request
    implicit val usingContext: Context = createContext(request)
    runEffect(system.map(
      system.either(handler.processRequest(requestMessage)),
      (handlerResult: Either[Throwable, HandlerResult[InputStream]]) => handlerResult.fold(
        error => serverError(error, response, client),
        result => {
          // Send the response
          val message = result.response.getOrElse(new ByteArrayInputStream(Array()))
          val status = result.errorCode.map(errorStatus).getOrElse(HttpStatus.OK_200)
          sendResponse(message, response, status, client)
        }
      )
    ))
    ()
  }

  private def serverError(error: Throwable, response: HttpServletResponse, client: String): Unit = {
    logger.error("Failed to process HTTP request", error, Map("Client" -> client))
    val message = Bytes.inputStream.to(Bytes.string.from(ResponseError.trace(error).mkString("\n")))
    val status = HttpStatus.INTERNAL_SERVER_ERROR_500
    sendResponse(message, response, status, client)
  }

  private def sendResponse(message: InputStream, response: HttpServletResponse, status: Int, client: String): Unit = {
    logger.debug("Sending HTTP response", Map("Client" -> client, "Status" -> status))
    response.setStatus(status)
    response.setContentType(handler.format.mediaType)
    val outputStream = response.getOutputStream
    IOUtils.copy(message, outputStream)
    outputStream.flush()
    logger.debug("Sent HTTP response", Map("Client" -> client, "Status" -> status))
  }

  private def createContext(request: HttpServletRequest): Context = {
    val headers = request.getHeaderNames.asScala.flatMap { name =>
      request.getHeaders(name).asScala.map(value => name -> value)
    }.toSeq
    HttpProperties(
      source = Some(request),
      method = Some(request.getMethod),
      headers = headers
    ).url(request.getRequestURI)
  }

  private def clientAddress(request: HttpServletRequest): String = {
    val forwardedFor = Option(request.getHeader(HttpHeader.X_FORWARDED_FOR.name))
    val address = request.getRemoteAddr
    Network.address(forwardedFor, address)
  }
}

case object JettyEndpoint {
  /** Request context type. */
  type Context = HttpProperties[HttpServletRequest]

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus: Int => Int = Map(
    ErrorType.ParseError -> HttpStatus.BAD_REQUEST_400,
    ErrorType.InvalidRequest -> HttpStatus.BAD_REQUEST_400,
    ErrorType.MethodNotFound -> HttpStatus.NOT_IMPLEMENTED_501,
    ErrorType.InvalidParams -> HttpStatus.BAD_REQUEST_400,
    ErrorType.InternalError -> HttpStatus.INTERNAL_SERVER_ERROR_500,
    ErrorType.IOError -> HttpStatus.INTERNAL_SERVER_ERROR_500,
    ErrorType.ApplicationError -> HttpStatus.INTERNAL_SERVER_ERROR_500
  ).withDefaultValue(HttpStatus.INTERNAL_SERVER_ERROR_500).map { case (errorType, status) =>
    errorType.code -> status
  }
}
