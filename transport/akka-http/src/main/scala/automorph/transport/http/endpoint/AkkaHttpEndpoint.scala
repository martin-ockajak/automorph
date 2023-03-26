package automorph.transport.http.endpoint

import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentType, HttpRequest, HttpResponse, RemoteAddress, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractClientIP, extractRequest, onComplete}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}
import automorph.transport.http.endpoint.AkkaHttpEndpoint.{Context, RpcHttpRequest}
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{
  ByteArrayOps, ByteBufferOps, EffectOps, InputStreamOps, StringOps, ThrowableOps, TryOps
}
import automorph.util.{Network, Random}
import java.io.InputStream
import java.util.concurrent.TimeUnit
import scala.collection.immutable.ListMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
 * Akka HTTP endpoint message transport plugin.
 *
 * Interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
 * - The response returned by the RPC handler is used as HTTP response body.
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
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy,
) extends Logging with EndpointTransport[Effect, Context, Behavior[RpcHttpRequest]] {

  private val log = MessageLog(logger, Protocol.Http.name)
  implicit private val system: EffectSystem[Effect] = effectSystem

  private val behavior = {
    val contentType = ContentType.parse(handler.mediaType).swap.map { errors =>
      new IllegalStateException(s"Invalid response content type: ${errors.map(_.toString).mkString("\n")}")
    }.swap.toTry.get

    Behaviors.receive[RpcHttpRequest] { case (actorContext, message) =>
      implicit val actorSystem: ActorSystem[Nothing] = actorContext.system
      implicit val executionContext: ExecutionContext = actorContext.executionContext
      // Log the request
      val requestId = Random.id
      val request = message.request
      val remoteAddress = message.clientAddress
      lazy val requestProperties = getRequestProperties(request, requestId, remoteAddress)
      log.receivedRequest(requestProperties)

      // Process the request
      request.entity.toStrict(readTimeout).map { requestEntity =>
        val requestBody = requestEntity.data.asByteBuffer.toInputStream
        handler.processRequest(requestBody, getRequestContext(request), requestId).either.map(
          _.fold(
            error =>
              sendErrorResponse(error, contentType, message.replyTo, remoteAddress, requestId, requestProperties),
            result => {
              // Send the response
              val responseBody = result.map(_.responseBody).getOrElse(Array[Byte]().toInputStream)
              val status = result.flatMap(_.exception).map(mapException).map(StatusCode.int2StatusCode)
                .getOrElse(StatusCodes.OK)
              sendResponse(
                responseBody,
                status,
                contentType,
                result.flatMap(_.context),
                message.replyTo,
                remoteAddress,
                requestId,
              )
            },
          )
        )
      }
      Behaviors.same
    }
  }

  override def adapter: Behavior[RpcHttpRequest] =
    behavior

  override def clone(handler: RequestHandler[Effect, Context]): AkkaHttpEndpoint[Effect] =
    copy(handler = handler)

  private def sendErrorResponse(
    error: Throwable,
    contentType: ContentType,
    replyTo: ActorRef[HttpResponse],
    remoteAddress: RemoteAddress,
    requestId: String,
    requestProperties: => Map[String, String],
  ): Unit = {
    log.failedProcessRequest(error, requestProperties)
    val responseBody = error.description.toInputStream
    sendResponse(responseBody, StatusCodes.InternalServerError, contentType, None, replyTo, remoteAddress, requestId)
  }

  private def sendResponse(
    responseBody: InputStream,
    statusCode: StatusCode,
    contentType: ContentType,
    responseContext: Option[Context],
    replyTo: ActorRef[HttpResponse],
    remoteAddress: RemoteAddress,
    requestId: String,
  ): Unit = {
    // Log the response
    val responseStatusCode = responseContext.flatMap(_.statusCode.map(StatusCode.int2StatusCode)).getOrElse(statusCode)
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(remoteAddress),
      "Status" -> responseStatusCode.intValue.toString,
    )
    log.sendingResponse(responseProperties)

    // Send the response
    Try {
      val baseResponse = setResponseContext(HttpResponse(), responseContext)
      val response = baseResponse.withStatus(responseStatusCode).withHeaders(baseResponse.headers)
        .withEntity(contentType, responseBody.toArray)
      replyTo.tell(response)
      log.sentResponse(responseProperties)
    }.onFailure(error => log.failedSendResponse(error, responseProperties)).get
  }

  private def setResponseContext(response: HttpResponse, responseContext: Option[Context]): HttpResponse =
    response.withHeaders(responseContext.toSeq.flatMap(_.headers).map { case (name, value) => RawHeader(name, value) })

  private def clientAddress(remoteAddress: RemoteAddress): String =
    remoteAddress.toOption.flatMap(address => Option(address.getHostAddress).map(Network.address(None, _)))
      .getOrElse("")

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
}

object AkkaHttpEndpoint extends Logging {

  private val log = MessageLog(logger, Protocol.Http.name)

  /** Request context type. */
  type Context = HttpContext[HttpRequest]

  /** Actor behavior message */
  final case class RpcHttpRequest(
    replyTo: ActorRef[HttpResponse],
    request: HttpRequest,
    clientAddress: RemoteAddress = RemoteAddress.Unknown,
  )

  /**
   * Creates an Akka HTTP route with the specified RPC endpoint actor.
   *
   * Interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
   * - The response returned by the RPC handler is used as HTTP response body.
   *
   * @see
   *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see
   *   [[https://doc.akka.io/docs/akka-http Library documentation]]
   * @see
   *   [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
   * @param handlerActor
   *   Akka actor with RPC endpoint behavior
   * @param requestTimeout
   *   HTTP request processing timeout
   * @param actorSystem
   *   Akka actor system
   * @return
   *   RPC handler Akka HTTP route
   */
  def route(
    handlerActor: ActorRef[RpcHttpRequest],
    requestTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS),
  )(implicit actorSystem: ActorSystem[?]): Route =
    // Process request
    extractRequest { httpRequest =>
      extractClientIP { remoteAddress =>
        implicit val timeout: Timeout = Timeout.durationToTimeout(requestTimeout)
        onComplete(handlerActor.ask[HttpResponse](RpcHttpRequest(_, httpRequest, remoteAddress)))(
          _.pureFold(
            error => {
              log.failedProcessRequest(error, Map())
              complete(InternalServerError, error.description)
            },
            httpResponse => complete(httpResponse),
          )
        )
      }
    }
}
