package automorph.transport.http.endpoint

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentType, HttpRequest, HttpResponse, RemoteAddress, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.{
  complete, extractClientIP, extractExecutionContext, extractMaterializer, extractRequest, onComplete
}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}
import automorph.transport.http.endpoint.AkkaHttpEndpoint.Context
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteBufferOps, EffectOps, InputStreamOps, StringOps, ThrowableOps, TryOps}
import automorph.util.{Network, Random}
import java.io.InputStream
import java.io.InputStream.nullInputStream
import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * Akka HTTP endpoint message transport plugin.
 *
 * Interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
 *   - The response returned by the RPC handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://doc.akka.io/docs/akka-http Library documentation]]
 * @see
 *   [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
 * @constructor
 *   Creates an Akka HTTP endpoint message transport plugin with specified effect system and request handler.
 * @param effectSystem
 *   effect system plugin
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @param readTimeout
 *   HTTP request read timeout
 * @param handler
 *   RPC request handler
 * @tparam Effect
 *   effect type
 */
case class AkkaHttpEndpoint[Effect[_]](
  effectSystem: EffectSystem[Effect],
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  readTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS),
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy[Effect, Context],
) extends Logging with EndpointTransport[Effect, Context, Route] {

  private lazy val contentType = ContentType.parse(handler.mediaType).swap.map { errors =>
    new IllegalStateException(s"Invalid message content type: ${errors.map(_.toString).mkString("\n")}")
  }.swap.toTry.get
  private val log = MessageLog(logger, Protocol.Http.name)
  private implicit val system: EffectSystem[Effect] = effectSystem

  def adapter: Route =
    extractRequest { httpRequest =>
      extractClientIP { remoteAddress =>
        extractMaterializer { implicit materializer =>
          extractExecutionContext { implicit executionContext =>
            onComplete(handle(httpRequest, remoteAddress))(
              _.fold(
                error => {
                  log.failedProcessRequest(error, Map())
                  complete(InternalServerError, error.description)
                },
                { case (httpResponse, responseProperties) =>
                  log.sentResponse(responseProperties)
                  complete(httpResponse)
                },
              )
            )
          }
        }
      }
    }

  override def clone(handler: RequestHandler[Effect, Context]): AkkaHttpEndpoint[Effect] =
    copy(handler = handler)

  private def handle(request: HttpRequest, remoteAddress: RemoteAddress)(
    implicit
    materializer: Materializer,
    executionContext: ExecutionContext
  ): Future[(HttpResponse, ListMap[String, String])] = {

    // Log the request
    val requestId = Random.id
    lazy val requestProperties = getRequestProperties(request, requestId, remoteAddress)
    log.receivedRequest(requestProperties)

    // Process the request
    val handleResult = Promise[(HttpResponse, ListMap[String, String])]()
    request.entity.toStrict(readTimeout).flatMap { requestEntity =>
      Try {
        val requestBody = requestEntity.data.asByteBuffer.toInputStream
        handler.processRequest(requestBody, getRequestContext(request), requestId).either.map { processRequestResult =>
          handleResult.success(processRequestResult.fold(
            error => createErrorResponse(error, contentType, remoteAddress, requestId, requestProperties),
            result => {
              // Create the response
              val responseBody = result.map(_.responseBody).getOrElse(nullInputStream())
              val status = result.flatMap(_.exception).map(mapException).map(StatusCode.int2StatusCode)
                .getOrElse(StatusCodes.OK)
              createResponse(responseBody, status, contentType, result.flatMap(_.context), remoteAddress, requestId)
            },
          ))
        }.runAsync
      }.foldError { error =>
        handleResult.success(createErrorResponse(error, contentType, remoteAddress, requestId, requestProperties))
      }
      handleResult.future
    }
  }

  private def createErrorResponse(
    error: Throwable,
    contentType: ContentType,
    remoteAddress: RemoteAddress,
    requestId: String,
    requestProperties: => Map[String, String],
  ): (HttpResponse, ListMap[String, String]) = {
    log.failedProcessRequest(error, requestProperties)
    val responseBody = error.description.toInputStream
    createResponse(responseBody, StatusCodes.InternalServerError, contentType, None, remoteAddress, requestId)
  }

  private def createResponse(
    responseBody: InputStream,
    statusCode: StatusCode,
    contentType: ContentType,
    responseContext: Option[Context],
    remoteAddress: RemoteAddress,
    requestId: String,
  ): (HttpResponse, ListMap[String, String]) = {
    // Log the response
    val responseStatusCode = responseContext.flatMap(_.statusCode.map(StatusCode.int2StatusCode)).getOrElse(statusCode)
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(remoteAddress),
      "Status" -> responseStatusCode.intValue.toString,
    )
    log.sendingResponse(responseProperties)

    // Send the response
    val baseResponse = setResponseContext(HttpResponse(), responseContext)
    val response = baseResponse.withStatus(responseStatusCode).withHeaders(baseResponse.headers)
      .withEntity(contentType, responseBody.toArray)
    response -> responseProperties
  }

  private def setResponseContext(response: HttpResponse, responseContext: Option[Context]): HttpResponse =
    response.withHeaders(responseContext.toSeq.flatMap(_.headers).map { case (name, value) => RawHeader(name, value) })

  private def getRequestContext(request: HttpRequest): Context =
    HttpContext(
      message = Some(request),
      method = Some(HttpMethod.valueOf(request.method.value)),
      headers = request.headers.map(header => header.name -> header.value),
    ).url(request.uri.toString)

  private def getRequestProperties(
    request: HttpRequest,
    requestId: String,
    remoteAddress: RemoteAddress,
  ): Map[String, String] =
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(remoteAddress),
      "URL" -> request.uri.toString,
      "Method" -> request.method.value,
    )

  private def clientAddress(remoteAddress: RemoteAddress): String =
    remoteAddress.toOption.flatMap(address => Option(address.getHostAddress).map(Network.address(None, _)))
      .getOrElse("")
}

object AkkaHttpEndpoint extends Logging {

  /** Request context type. */
  type Context = HttpContext[HttpRequest]
}
