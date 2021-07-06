package automorph.server.http

import jakarta.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import java.io.{ByteArrayInputStream, InputStream}
import automorph.Handler
import automorph.handler.{Bytes, HandlerResult}
import automorph.log.Logging
import automorph.protocol.{ErrorType, ResponseError}
import automorph.server.http.JettyJsonRpcServlet.defaultErrorStatus
import automorph.spi.Codec
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.http.{HttpHeader, HttpStatus}

/**
 * JSON-RPC HTTP servlet for Jetty web server.
 *
 * The servlet interprets HTTP request body as a JSON-RPC request and processes it using the specified JSON-RPC handler.
 * The response returned by the JSON-RPC handler is used as HTTP response body.
 *
 * @see [[https://www.eclipse.org/jetty/ Documentation]]
 * @see [[https://www.eclipse.org/jetty/javadoc/jetty-11/index.html API]]
 * @constructor Create a JSON-RPC HTTP servlet for Jetty web server using the specified JSON-RPC request ''handler''.
 * @param handler JSON-RPC request handler
 * @param runEffect asynchronous effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status code mapping function
 * @tparam Node message format node representation type
 * @tparam ExactCodec message codec plugin type
 * @tparam Effect effect type
 */
final case class JettyJsonRpcServlet[Node, ExactCodec <: Codec[Node], Effect[_]](
  handler: Handler[Node, ExactCodec, Effect, HttpServletRequest],
  runEffect: Effect[Any] => Unit,
  errorStatus: Int => Int = defaultErrorStatus
) extends HttpServlet with Logging {

  private val backend = handler.backend

  override def service(request: HttpServletRequest, response: HttpServletResponse): Unit = {
    // Receive the request
    val client = clientAddress(request)
    logger.debug("Received HTTP request", Map("Client" -> client))
    val requestMessage: InputStream = request.getInputStream

    // Process the request
    implicit val usingContext = request
    runEffect(backend.map(
      backend.either(handler.processRequest(requestMessage)),
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
  }

  private def serverError(error: Throwable, response: HttpServletResponse, client: String): Unit = {
    val message = Bytes.inputStreamBytes.to(Bytes.stringBytes.from(ResponseError.trace(error).mkString("\n")))
    val status = HttpStatus.INTERNAL_SERVER_ERROR_500
    logger.error("Failed to process HTTP request", error, Map("Client" -> client))
    sendResponse(message, response, status, client)
  }

  private def sendResponse(message: InputStream, response: HttpServletResponse, status: Int, client: String): Unit = {
    logger.debug("Sending HTTP response", Map("Client" -> client, "Status" -> status.toString))
    response.setStatus(status)
    response.setContentType(handler.codec.mediaType)
    val outputStream = response.getOutputStream
    IOUtils.copy(message, outputStream)
    outputStream.flush()
    logger.debug("Sent HTTP response", Map("Client" -> client, "Status" -> status.toString))
  }

  private def clientAddress(request: HttpServletRequest): String = {
    Option(request.getHeader(HttpHeader.X_FORWARDED_FOR.name)).flatMap(_.split(",", 2).headOption).getOrElse {
      val address = request.getRemoteAddr.toString.split("/", 2).reverse.head
      address.replaceAll("/", "").split(":").init.mkString(":")
    }
  }
}

case object JettyJsonRpcServlet {

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus = Map(
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
