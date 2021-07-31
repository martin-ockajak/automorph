package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.protocol.jsonrpc.ResponseError
import automorph.spi.{EndpointMessageTransport, MessageFormat}
import automorph.transport.http.Http
import automorph.util.Bytes
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
case object TapirHttpEndpoint extends Logging with EndpointMessageTransport {

  /** Request context type. */
  type Context = Http[Unit]

  /** Endpoint request type. */
  type RequestType = (Array[Byte], List[String], QueryParams, List[Header], List[Cookie], Option[String])

  /**
   * Creates a Tapir HTTP endpoint with the specified RPC request ''handler''.
   *
   * The endpoint interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
   * The response returned by the RPC handler is used as HTTP response body.
   *
   * @see [[https://tapir.softwaremill.com/ Documentation]]
   * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
   * @param handler RPC request handler
   * @param method HTTP method to server
   * @param errorStatusCode maps a JSON-RPC error to a corresponding HTTP status code
   * @tparam Effect effect type
   * @return Tapir HTTP endpoint
   */
  def apply[Effect[_]](
    handler: Handler.AnyFormat[Effect, Context],
    method: Method,
    errorStatusCode: Int => Int = Http.defaultErrorStatusCode
  ): ServerEndpoint[RequestType, Unit, (Array[Byte], StatusCode), Any, Effect] = {
    val system = handler.system
    val contentType = Header.contentType(MediaType.parse(handler.format.mediaType).getOrElse {
      throw new IllegalArgumentException(s"Invalid content type: ${handler.format.mediaType}")
    })
    endpoint.method(method).in(byteArrayBody).in(paths).in(queryParams).in(headers).in(cookies).in(clientIp)
      .out(byteArrayBody).out(header(contentType)).out(statusCode)
      .serverLogic { case (requestMessage, paths, queryParams, headers, cookies, clientIp) =>
        // Receive the request
        val client = clientAddress(clientIp)
        logger.debug("Received HTTP request", Map("Client" -> client, "Size" -> requestMessage.length))

        // Process the request
        implicit val usingContext: Context = createContext(method, paths, queryParams, headers)
        system.map(
          system.either(handler.processRequest(requestMessage)),
          (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
            handlerResult.fold(
              error => Right(serverError(error, requestMessage, client)),
              result => {
                // Send the response
                val message = result.response.getOrElse(Array[Byte]())
                val status = result.errorCode.map(errorStatusCode).map(StatusCode.apply).getOrElse(StatusCode.Ok)
                Right(createResponse(message, status, client))
              }
            )
        )
      }
  }

  private def serverError(error: Throwable, request: Array[Byte], client: String): (Array[Byte], StatusCode) = {
    logger.error("Failed to process HTTP request", error, Map("Client" -> client, "Size" -> request.length))
    val message = Bytes.string.from(ResponseError.trace(error).mkString("\n")).unsafeArray
    val status = StatusCode.InternalServerError
    createResponse(message, status, client)
  }

  private def createResponse(message: Array[Byte], status: StatusCode, client: String): (Array[Byte], StatusCode) = {
    logger.debug("Sending HTTP response", Map("Client" -> client, "Status" -> status, "Size" -> message.length))
    (message, status)
  }

  private def createContext(
    method: Method,
    paths: List[String],
    queryParams: QueryParams,
    headers: List[Header],
  ): Context = {
    Http(
      source = Some(()),
      method = Some(method.method),
      path = Some(paths.mkString("/")),
      query = Some(queryParams.toString),
      headers = headers.map(header => header.name -> header.value).toSeq
    )
  }

  private def clientAddress(clientIp: Option[String]): String = clientIp.getOrElse("[unknown]")
}
