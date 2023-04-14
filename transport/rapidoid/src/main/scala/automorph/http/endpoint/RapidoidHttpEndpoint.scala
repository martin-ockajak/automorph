package automorph.transport.http.endpoint

import automorph.util.Extensions.{ByteArrayOps, InputStreamOps, StringOps}
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.util.Try

/**
 * Rapidoid HTTP endpoint message transport plugin.
 *
 * The service interprets HTTP request body as a RPC request and processes it with the specified RPC handler. The
 * response returned by the RPC handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://rapidoid.org Library documentation]]
 * @see
 *   [[https://rapidoid.org/latest/api/org/rapidoid/ API]]
 * @constructor
 *   Creates a Rapidoid HTTP service with the specified RPC request handler.
 * @param handler
 *   RPC request handler
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @tparam Effect
 *   effect type
 */
final case class RapidoidHttpEndpoint[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
) extends ReqHandler with Logging with EndpointMessageTransport {

  private val statusOk = 200
  private val statusInternalServerError = 500
  private val log = MessageLog(logger, Protocol.Http.name)
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private implicit val system: EffectSystem[Effect] = genericHandler.system

  override def execute(request: Req): AnyRef = {
    // Log the request
    val requestId = Random.id
    lazy val requestProperties = getRequestProperties(request, requestId)
    log.receivedRequest(requestProperties)
    if (!request.isAsync) { request.async() }

    // Process the request
    val requestBody = request.body.toInputStream
    val response = genericHandler.processRequest(requestBody, getRequestContext(request), requestId)
    response.either.map(
      _.fold(
        error => sendErrorResponse(error, request, requestId, requestProperties),
        result => {
          // Send the response
          val responseBody = result.responseBody.getOrElse(nullInputStream())
          val statusCode = result.exception.map(mapException).getOrElse(statusOk)
          sendResponse(responseBody, statusCode, result.context, request, requestId)
        },
      )
    )
    request
  }

  private def sendErrorResponse(
    error: Throwable,
    request: Req,
    requestId: String,
    requestProperties: => Map[String, String],
  ): Unit = {
    log.failedProcessRequest(error, requestProperties)
    val responseBody = error.description.toInputStream
    sendResponse(responseBody, statusInternalServerError, None, request, requestId)
  }

  private def sendResponse(
    responseBody: InputStream,
    statusCode: Int,
    responseContext: Option[Context],
    request: Req,
    requestId: String,
  ): Unit = {
    // Log the response
    val responseStatusCode = responseContext.flatMap(_.statusCode).getOrElse(statusCode)
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "Status" -> responseStatusCode.toString,
    )
    log.sendingResponse(responseProperties)

    // Send the response
    val response = request.response
    Try {
      setResponseContext(response, responseContext)
        .header(HttpHeaders.CONTENT_TYPE.name, genericHandler.protocol.codec.mediaType).code(responseStatusCode)
        .body(responseBody.toArray).done()
      log.sentResponse(responseProperties)
    }.onFailure { error =>
      response.done()
      log.failedSendResponse(error, responseProperties)
    }.get
  }

  private def setResponseContext(response: Resp, responseContext: Option[Context]): Resp =
    responseContext.toSeq.flatMap(_.headers).foldLeft(response) { case (current, (name, value)) =>
      response.header(name, value)
    }

  private def getRequestContext(request: Req): Context =
    HttpContext(
      transport = Some(request),
      method = Some(HttpMethod.valueOf(request.verb)),
      headers = request.headers.asScala.toSeq,
    ).url(request.uri)

  private def getRequestProperties(request: Req, requestId: String): Map[String, String] =
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "URL" -> request.uri,
      "Method" -> request.verb,
    )

  private def clientAddress(request: Req): String =
    Network.address(None, request.realIpAddress)
}

case object RapidoidHttpEndpoint {

  /** Request context type. */
  type Context = HttpContext[Req]
}
