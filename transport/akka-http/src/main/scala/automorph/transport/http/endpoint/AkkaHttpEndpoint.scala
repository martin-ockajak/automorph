package automorph.transport.http.endpoint

import akka.actor.typed.scaladsl.AskPattern.{schedulerFromActorSystem, Askable}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentType, HttpRequest, HttpResponse, RemoteAddress, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractClientIP, extractRequest, onComplete}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import automorph.Types
import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, ByteBufferOps, EffectOps, InputStreamOps, StringOps, ThrowableOps, TryOps}
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
 * The endpoint interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://doc.akka.io/docs/akka-http Library documentation]]
 * @see [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
 */
object AkkaHttpEndpoint extends Logging with EndpointMessageTransport {

  /** Request context type. */
  type Context = HttpContext[HttpRequest]

  private val log = MessageLog(logger, Protocol.Http.name)

  /**
   * Creates an Akka HTTP route with the specified RPC request handler.
   *
   * The route interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
   * The response returned by the RPC handler is used as HTTP response body.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://doc.akka.io/docs/akka-http Library documentation]]
   * @see [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
   * @param handlerActor Akka actor with RPC handler behavior
   * @param requestTimeout HTTP request processing timeout
   * @param actorSystem Akka actor system
   * @return RPC handler Akka HTTP route
   */
  def apply(
    handlerActor: ActorRef[RpcHttpRequest],
    requestTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS)
  )(implicit actorSystem: ActorSystem[_]): Route = {

    // Process request
    extractRequest { httpRequest =>
      extractClientIP { remoteAddress =>
        implicit val timeout: Timeout = Timeout.durationToTimeout(requestTimeout)
        onComplete(handlerActor.ask[HttpResponse](RpcHttpRequest(_, httpRequest, remoteAddress)))(_.pureFold(
          error => {
            log.failedProcessRequest(error, Map())
            complete(InternalServerError, error.description)
          },
          httpResponse => complete(httpResponse)
        ))
      }
    }
  }

  /**
   * Creates an Akka actor behavior with the specified RPC request handler.
   *
   * The actor behavior interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
   * The response returned by the RPC handler is used as HTTP response body.
   *
   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
   * @see [[https://doc.akka.io/docs/akka-http Library documentation]]
   * @see [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
   * @param handler RPC request handler
   * @param mapException maps an exception to a corresponding HTTP status code
   * @param readTimeout request body read timeout
   * @tparam Effect effect type
   * @return RPC handler Akka actor behavior
   */
  def behavior[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
    readTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS)
  ): Behavior[RpcHttpRequest] = {
    val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
    implicit val system: EffectSystem[Effect] = genericHandler.system
    val contentType = ContentType.parse(genericHandler.protocol.codec.mediaType).swap.map { errors =>
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
        genericHandler.processRequest(requestBody, getRequestContext(request), requestId).either.map(_.fold(
          error => sendErrorResponse(error, contentType, message.replyTo, remoteAddress, requestId, requestProperties),
          result => {
            // Send the response
            val responseBody = result.responseBody.getOrElse(Array[Byte]().toInputStream)
            val statusCode =
              result.exception.map(mapException).map(StatusCode.int2StatusCode).getOrElse(StatusCodes.OK)
            sendResponse(
              responseBody,
              statusCode,
              contentType,
              result.context,
              message.replyTo,
              remoteAddress,
              requestId
            )
          }
        ))
      }
      Behaviors.same
    }
  }

  private def sendErrorResponse(
    error: Throwable,
    contentType: ContentType,
    replyTo: ActorRef[HttpResponse],
    remoteAddress: RemoteAddress,
    requestId: String,
    requestProperties: => Map[String, String]
  ): Unit = {
    log.failedProcessRequest(error, requestProperties)
    val responseBody = error.description.toInputStream
    sendResponse(responseBody, StatusCodes.InternalServerError, contentType, None, replyTo, remoteAddress, requestId)
  }

  private def sendResponse[Effect[_]](
    responseBody: InputStream,
    statusCode: StatusCode,
    contentType: ContentType,
    responseContext: Option[Context],
    replyTo: ActorRef[HttpResponse],
    remoteAddress: RemoteAddress,
    requestId: String
  ): Unit = {
    // Log the response
    val responseStatusCode = responseContext.flatMap(_.statusCode.map(StatusCode.int2StatusCode)).getOrElse(statusCode)
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(remoteAddress),
      "Status" -> responseStatusCode.intValue.toString
    )
    log.sendingResponse(responseProperties)

    // Send the response
    Try {
      val baseResponse = setResponseContext(HttpResponse(), responseContext)
      val response = baseResponse
        .withStatus(responseStatusCode)
        .withHeaders(baseResponse.headers)
        .withEntity(contentType, responseBody.toArray)
      replyTo.tell(response)
      log.sentResponse(responseProperties)
    }.onFailure { error =>
      log.failedSendResponse(error, responseProperties)
    }.get
  }

  private def getRequestContext(request: HttpRequest): Context =
    HttpContext(
      transport = Some(request),
      method = Some(HttpMethod.valueOf(request.method.value)),
      headers = request.headers.map(header => header.name -> header.value)
    ).url(request.uri.toString)

  private def setResponseContext(response: HttpResponse, responseContext: Option[Context]): HttpResponse =
    response.withHeaders(responseContext.toSeq.flatMap(_.headers).map { case (name, value) =>
      RawHeader(name, value)
    })

  private def getRequestProperties(
    request: HttpRequest,
    requestId: String,
    remoteAddress: RemoteAddress
  ): Map[String, String] =
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(remoteAddress),
      "URL" -> request.uri.toString,
      "Method" -> request.method.value
    )

  private def clientAddress(remoteAddress: RemoteAddress): String = {
    remoteAddress.toOption.flatMap { address =>
      Option(address.getHostAddress).map(Network.address(None, _))
    }.getOrElse("")
  }

  /** Actor behavior message */
  final case class RpcHttpRequest(
    replyTo: ActorRef[HttpResponse],
    request: HttpRequest,
    clientAddress: RemoteAddress = RemoteAddress.Unknown
  )
}
