package automorph.transport.http.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.endpoint.VertxHttpEndpoint.Context
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{EffectOps, ThrowableOps}
import automorph.util.{Bytes, MessageLog, Network, Random}
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.{HttpHeaders, HttpServerRequest, HttpServerResponse, ServerWebSocket}
import scala.collection.immutable.{ArraySeq, ListMap}
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala}

/**
 * Vert.x HTTP endpoint message transport plugin.
 *
 * The handler interprets HTTP request body as an RPC request and processes it using the specified RPC request handler.
 * The response returned by the RPC request handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://vertx.io Library documentation]]
 * @see [[https://vertx.io/docs/apidocs/index.html API]]
 * @constructor Creates an Vert.x HTTP handler with specified RPC request handler.
 * @param handler RPC request handler
 * @param mapException maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class VertxHttpEndpoint[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode
) extends Handler[HttpServerRequest] with Logging with EndpointMessageTransport {

  private val statusOk = 200
  private val statusInternalServerError = 500
  private val headerXForwardedFor = "X-Forwarded-For"
  private val log = MessageLog(logger, Protocol.Http.name)
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  implicit private val system: EffectSystem[Effect] = genericHandler.system

  override def handle(request: HttpServerRequest): Unit = {
    // Log the request
    val requestId = Random.id
    lazy val requestProperties = getRequestProperties(request, requestId)
    log.receivingRequest(requestProperties)
    request.bodyHandler { buffer =>
      val requestBody = Bytes.byteArray.from(buffer.getBytes)
      log.receivedRequest(requestProperties)

      // Process the request
      genericHandler.processRequest(requestBody, getRequestContext(request), requestId).either.map(_.fold(
        error => sendErrorResponse(error, request, requestId, requestProperties),
        result => {
          // Send the response
          val responseBody = result.responseBody.getOrElse(new ArraySeq.ofByte(Array()))
          val statusCode = result.exception.map(mapException).getOrElse(statusOk)
          sendResponse(responseBody, statusCode, result.context, request, requestId)
        }
      )).run
    }.end().onFailure { error =>
      sendErrorResponse(error, request, requestId, requestProperties)
    }
    ()
  }

  private def sendErrorResponse(
    error: Throwable,
    request: HttpServerRequest,
    requestId: String,
    requestProperties: => Map[String, String]
  ): Unit = {
    log.failedProcessRequest(error, requestProperties)
    val responseBody = Bytes.string.from(error.trace.mkString("\n"))
    sendResponse(responseBody, statusInternalServerError, None, request, requestId)
  }

  private def sendResponse(
    responseBody: ArraySeq.ofByte,
    statusCode: Int,
    responseContext: Option[Context],
    request: HttpServerRequest,
    requestId: String
  ): Unit = {
    // Log the response
    val responseStatusCode = responseContext.flatMap(_.statusCode).getOrElse(statusCode)
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "Status" -> responseStatusCode.toString
    )
    log.sendingResponse(responseProperties)

    // Send the response
    setResponseContext(request.response, responseContext)
      .putHeader(HttpHeaders.CONTENT_TYPE, genericHandler.protocol.codec.mediaType)
      .setStatusCode(statusCode)
      .end(Buffer.buffer(Bytes.byteArray.to(responseBody))).onSuccess { _ =>
        log.sentResponse(responseProperties)
      }.onFailure { error =>
        log.failedSendResponse(error, responseProperties)
      }
    ()
  }

  private def getRequestContext(request: HttpServerRequest): Context = {
    val headers = request.headers.entries.asScala.map { entry =>
      entry.getKey -> entry.getValue
    }.toSeq
    HttpContext(
      transport = Some(Left(request).withRight[ServerWebSocket]),
      method = Some(HttpMethod.valueOf(request.method.name)),
      headers = headers
    ).url(request.absoluteURI)
  }

  private def setResponseContext(response: HttpServerResponse, responseContext: Option[Context]): HttpServerResponse = {
    val headers = responseContext.toSeq.flatMap(_.headers)
    headers.foldLeft(response) { case (current, (name, value)) =>
      current.putHeader(name, value)
    }
  }

  private def getRequestProperties(request: HttpServerRequest, requestId: String): Map[String, String] = {
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "URL" -> request.absoluteURI,
      "Method" -> request.method.name
    )
  }

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
