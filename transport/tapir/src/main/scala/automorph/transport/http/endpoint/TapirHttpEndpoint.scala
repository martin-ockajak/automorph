package automorph.transport.http.endpoint

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.util.Extensions.ThrowableOps
import automorph.util.{Bytes, Random}
import sttp.model.{Header, MediaType, Method, QueryParams, StatusCode}
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.{CodecFormat, byteArrayBody, clientIp, endpoint, header, headers, paths, queryParams, statusCode}

/**
 * Tapir HTTP endpoint message transport plugin.
 *
 * The endpoint interprets HTTP request body as an RPC request and processes it using the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://tapir.softwaremill.com Library documentation]]
 * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
 */
object TapirHttpEndpoint extends Logging with EndpointMessageTransport {

  /** Request context type. */
  type Context = Http[Unit]

  /** Endpoint request type. */
  type RequestType = (Array[Byte], List[String], QueryParams, List[Header], Option[String])

  /**
   * Creates a Tapir HTTP endpoint with the specified RPC request handler.
   *
   * The endpoint interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
   * The response returned by the RPC handler is used as HTTP response body.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://tapir.softwaremill.com Library documentation]]
   * @see [[https://javadoc.io/doc/com.softwaremill.tapir/tapir-core_2.13/latest/index.html API]]
   * @param handler RPC request handler
   * @param method HTTP method to server
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @tparam Effect effect type
   * @return Tapir HTTP endpoint
   */
  def apply[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    method: Method,
    exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
  ): ServerEndpoint[RequestType, Unit, (Array[Byte], StatusCode), Any, Effect] = {
    val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
    val system = genericHandler.system
    val contentType = Header.contentType(MediaType.parse(genericHandler.protocol.codec.mediaType).getOrElse {
      throw new IllegalArgumentException(s"Invalid content type: ${genericHandler.protocol.codec.mediaType}")
    })
    endpoint.method(method).in(byteArrayBody).in(paths).in(queryParams).in(headers).in(clientIp)
      .out(byteArrayBody).out(header(contentType)).out(statusCode)
      .serverLogic { case (requestMessage, paths, queryParams, headers, clientIp) =>
        // Log the request
        val requestId = Random.id
        lazy val requestProperties = extractRequestProperties(clientIp, Some(method), requestId)
        logger.debug("Received HTTP request", requestProperties)

        // Process the request
        implicit val usingContext: Context = requestContext(paths, queryParams, headers, Some(method))
        system.map(
          system.either(genericHandler.processRequest(requestMessage, requestId, Some(urlPath(paths)))),
          (handlerResult: Either[Throwable, HandlerResult[Array[Byte], Context]]) =>
            handlerResult.fold(
              error => Right(serverError(error, clientIp, requestId, requestProperties)),
              result => {
                // Send the response
                val message = result.responseBody.getOrElse(Array[Byte]())
                val status = result.exception.map(exceptionToStatusCode).map(StatusCode.apply).getOrElse(StatusCode.Ok)
                Right(createResponse(message, status, clientIp, requestId))
              }
            )
        )
      }
  }

  private[automorph] def serverError(
    error: Throwable,
    clientIp: Option[String],
    requestId: String,
    requestProperties: => Map[String, String]
  ): (Array[Byte], StatusCode) = {
    logger.error("Failed to process HTTP request", error, requestProperties)
    val message = Bytes.string.from(error.trace.mkString("\n")).unsafeArray
    val status = StatusCode.InternalServerError
    createResponse(message, status, clientIp, requestId)
  }

  private[automorph] def createResponse(
    message: Array[Byte],
    status: StatusCode,
    clientIp: Option[String],
    requestId: String
  ): (Array[Byte], StatusCode) = {
    // Log the response
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(clientIp),
      "Status" -> statusCode.toString
    )
    logger.debug("Sending HTTP response", responseDetails)
    (message, status)
  }

  private[automorph] def requestContext(
    paths: List[String],
    queryParams: QueryParams,
    headers: List[Header],
    method: Option[Method]
  ): Context =
    Http(
      base = Some(()),
      method = method.map(_.method),
      path = Some(urlPath(paths)),
      parameters = queryParams.toSeq,
      headers = headers.map(header => header.name -> header.value).toSeq
    )

  private[automorph] def urlPath(paths: List[String]): String = paths match {
    case Nil => "/"
    case items => items.mkString("/")
  }

  private[automorph] def extractRequestProperties(
    clientIp: Option[String],
    method: Option[Method],
    requestId: String
  ): Map[String, String] = Map(
    LogProperties.requestId -> requestId,
    "Client" -> clientAddress(clientIp)
  ) ++ method.map("Method" -> _.toString)

  private def clientAddress(clientIp: Option[String]): String = clientIp.getOrElse("[unknown]")
}
