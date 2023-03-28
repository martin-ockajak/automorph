package automorph.transport.http.endpoint

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}
import automorph.transport.http.endpoint.VertxHttpEndpoint.Context
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, StringOps, ThrowableOps}
import automorph.util.{Network, Random}
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpHeaders, HttpServerRequest, HttpServerResponse, ServerWebSocket}
import java.io.InputStream
import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

/**
 * Vert.x HTTP endpoint message transport plugin.
 *
 * Interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 *   - The response returned by the RPC request handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://vertx.io Library documentation]]
 * @see
 *   [[https://vertx.io/docs/apidocs/index.html API]]
 * @constructor
 *   Creates a Vert.x HTTP endpoint message transport plugin with specified effect system and request handler.
 * @param effectSystem
 *   effect system plugin
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @param handler
 *   RPC request handler
 * @tparam Effect
 *   effect type
 */
final case class VertxHttpEndpoint[Effect[_]](
  effectSystem: EffectSystem[Effect],
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy[Effect, Context],
) extends Handler[HttpServerRequest] with Logging with EndpointTransport[Effect, Context, Handler[HttpServerRequest]] {

  private val statusOk = 200
  private val statusInternalServerError = 500
  private val headerXForwardedFor = "X-Forwarded-For"
  private val log = MessageLog(logger, Protocol.Http.name)
  implicit private val system: EffectSystem[Effect] = effectSystem

  override def adapter: Handler[HttpServerRequest] =
    this

  override def clone(handler: RequestHandler[Effect, Context]): VertxHttpEndpoint[Effect] =
    copy(handler = handler)

  override def handle(request: HttpServerRequest): Unit = {
    // Log the request
    val requestId = Random.id
    lazy val requestProperties = getRequestProperties(request, requestId)
    log.receivingRequest(requestProperties)
    request.bodyHandler { buffer =>
      Try {
        val requestBody = buffer.getBytes.toInputStream
        log.receivedRequest(requestProperties)

        // Process the request
        handler.processRequest(requestBody, getRequestContext(request), requestId).either.map(
          _.fold(
            error => sendErrorResponse(error, request, requestId, requestProperties),
            result => {
              // Send the response
              val responseBody = result.map(_.responseBody).getOrElse(Array[Byte]().toInputStream)
              val status = result.flatMap(_.exception).map(mapException).getOrElse(statusOk)
              sendResponse(responseBody, status, result.flatMap(_.context), request, requestId)
            },
          )
        ).runAsync
      }.failed.foreach { error =>
        sendErrorResponse(error, request, requestId, requestProperties)
      }
    }
    ()
  }

  private def sendErrorResponse(
    error: Throwable,
    request: HttpServerRequest,
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
    request: HttpServerRequest,
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
    setResponseContext(request.response, responseContext)
      .putHeader(HttpHeaders.CONTENT_TYPE, handler.mediaType).setStatusCode(statusCode)
      .end(Buffer.buffer(responseBody.toArray)).onSuccess(_ => log.sentResponse(responseProperties)).onFailure {
        error => log.failedSendResponse(error, responseProperties)
      }
    ()
  }

  private def getRequestContext(request: HttpServerRequest): Context = {
    val headers = request.headers.entries.asScala.map(entry => entry.getKey -> entry.getValue).toSeq
    HttpContext(
      message = Some(Left(request).withRight[ServerWebSocket]),
      method = Some(HttpMethod.valueOf(request.method.name)),
      headers = headers,
    ).url(request.absoluteURI)
  }

  private def setResponseContext(response: HttpServerResponse, responseContext: Option[Context]): HttpServerResponse =
    responseContext.toSeq.flatMap(_.headers).foldLeft(response) { case (current, (name, value)) =>
      current.putHeader(name, value)
    }

  private def getRequestProperties(request: HttpServerRequest, requestId: String): Map[String, String] =
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "URL" -> request.absoluteURI,
      "Method" -> request.method.name,
    )

  private def clientAddress(request: HttpServerRequest): String = {
    val forwardedFor = Option(request.headers().get(headerXForwardedFor))
    val address = Option(request.remoteAddress.hostName).orElse(Option(request.remoteAddress.hostAddress)).getOrElse("")
    Network.address(forwardedFor, address)
  }
}

object VertxHttpEndpoint {

  /** Request context type. */
  type Context = HttpContext[Either[HttpServerRequest, ServerWebSocket]]
}
