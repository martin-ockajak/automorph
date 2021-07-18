package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.{Bytes, HandlerResult}
import automorph.log.Logging
import automorph.protocol.{ErrorType, ResponseError}
import automorph.spi.{EndpointMessageTransport, MessageFormat}
import sttp.model.headers.Cookie
import sttp.model.{Header, MediaType, Method, QueryParams, StatusCode}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{byteArrayBody, clientIp, cookies, endpoint, header, headers, paths, queryParams, statusCode}

/**
 * Tapir endpoint endpoint transport plugin using HTTP as message transport protocol.
 *
 * The endpoint interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://tapir.softwaremill.com Documentation]]
 */
case object TapirEndpoint extends Logging with EndpointMessageTransport {

  /** Request context type. */
  type Context = Request

  /** Endpoint request type. */
  type RequestType = (Array[Byte], List[String], QueryParams, List[Header], List[Cookie], Option[String])

  /**
   * Creates a Tapir endpoint with the specified RPC request ''handler''.
   *
   * The endpoint interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
   * The response returned by the RPC handler is used as HTTP response body.
   *
   * @see [[https://tapir.softwaremill.com/ Documentation]]
   * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
   * @param handler RPC request handler
   * @param method HTTP method to server
   * @param errorStatus JSON-RPC error code to HTTP status code mapping function
   * @tparam Node message node type
   * @tparam Effect effect type
   * @return Tapir HTTP server endpoint
   */
  def apply[Node, Effect[_]](
    handler: Handler[Node, _ <: MessageFormat[Node], Effect, Request],
    method: Method,
    errorStatus: Int => StatusCode = defaultErrorStatus
  ): ServerEndpoint[RequestType, Unit, (Array[Byte], StatusCode), Any, Effect] = {
    val system = handler.system
    val contentType = Header.contentType(MediaType.parse(handler.format.mediaType).getOrElse {
      throw new IllegalArgumentException(s"Invalid content type: ${handler.format.mediaType}")
    })
    endpoint.method(method).in(byteArrayBody).in(paths).in(queryParams).in(headers).in(cookies).in(clientIp)
      .out(byteArrayBody).out(header(contentType)).out(statusCode)
      .serverLogic { case (requestMessage, paths, queryParams, headers, cookies, clientIp) =>
        // Receive the request
        val request = Request(paths, queryParams, headers, cookies, clientIp)
        val client = clientAddress(request.clientIp)
        logger.debug("Received HTTP request", Map("Client" -> client, "Size" -> requestMessage.length))

        // Process the request
        implicit val usingContext: Request = request
        system.map(
          system.either(handler.processRequest(requestMessage)),
          (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
            handlerResult.fold(
              error => Right(serverError(error, requestMessage, client)),
              result => {
                // Send the response
                val message = result.response.getOrElse(Array[Byte]())
                val status = result.errorCode.map(errorStatus).getOrElse(StatusCode.Ok)
                Right(createResponse(message, status, client))
              }
            )
        )
      }
  }

  private def serverError(error: Throwable, request: Array[Byte], client: String): (Array[Byte], StatusCode) = {
    val message = Bytes.stringBytes.from(ResponseError.trace(error).mkString("\n")).unsafeArray
    val status = StatusCode.InternalServerError
    logger.error("Failed to process HTTP request", error, Map("Client" -> client, "Size" -> request.length))
    createResponse(message, status, client)
  }

  private def createResponse(message: Array[Byte], status: StatusCode, client: String): (Array[Byte], StatusCode) = {
    logger.debug("Sending HTTP response", Map("Client" -> client, "Status" -> status, "Size" -> message.length))
    (message, status)
  }

  private def clientAddress(clientIp: Option[String]): String =
    clientIp.getOrElse("[unknown]")

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus: Int => StatusCode = Map(
    ErrorType.ParseError -> StatusCode.BadRequest,
    ErrorType.InvalidRequest -> StatusCode.BadRequest,
    ErrorType.MethodNotFound -> StatusCode.NotImplemented,
    ErrorType.InvalidParams -> StatusCode.BadRequest,
    ErrorType.InternalError -> StatusCode.InternalServerError,
    ErrorType.IOError -> StatusCode.InternalServerError,
    ErrorType.ApplicationError -> StatusCode.InternalServerError
  ).withDefaultValue(StatusCode.InternalServerError).map { case (errorType, status) =>
    errorType.code -> status
  }
}

/**
 * Tapir endpoint HTTP request context.
 *
 * @param paths URL path components separated by '/'
 * @param queryParams URL query parameters
 * @param headers HTTP headers
 * @param cookies HTTP cookies
 */
final case class Request(
  paths: List[String],
  queryParams: QueryParams,
  headers: List[Header],
  cookies: List[Cookie],
  clientIp: Option[String]
)
