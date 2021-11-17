//package automorph.transport.http.endpoint
//
//import akka.actor.typed.scaladsl.Behaviors
//import akka.actor.typed.{ActorRef, Behavior}
//import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpRequest, HttpResponse, ResponseEntity, StatusCode, StatusCodes}
//import akka.stream.ActorMaterializer
//import automorph.Types
//import automorph.log.{LogProperties, Logging, MessageLog}
//import automorph.spi.EffectSystem
//import automorph.spi.transport.EndpointMessageTransport
//import automorph.transport.http.endpoint.AkkaHttpEndpoint.Context
//import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
//import automorph.util.Extensions.{EffectOps, ThrowableOps, TryOps}
//import automorph.util.{Bytes, Network, Random}
//import java.util.concurrent.TimeUnit
//import scala.collection.immutable.{ArraySeq, ListMap}
//import scala.concurrent.duration.FiniteDuration
//import scala.jdk.CollectionConverters.{ListHasAsScala, MapHasAsScala}
//import scala.util.Try
//
///**
// * Akka HTTP endpoint message transport plugin.
// *
// * The service interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
// * The response returned by the RPC handler is used as HTTP response body.
// *
// * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
// * @see [[https://doc.akka.io/docs/akka-http Library documentation]]
// * @see [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
// * @constructor Creates a Akka HTTP actor behavior with the specified RPC request handler.
// * @param handler RPC request handler
// * @param mapException maps an exception to a corresponding HTTP status code
// * @tparam Effect effect type
// */
//object AkkaHttpEndpoint extends Logging with EndpointMessageTransport {
//
//  /** Request context type. */
//  type Context = HttpContext[HttpRequest]
//
//  private val log = MessageLog(logger, Protocol.Http.name)
//
//  /**
//   * Creates a Akka HTTP actor behavior with the specified RPC request handler.
//   *
//   * The endpoint interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
//   * The response returned by the RPC handler is used as HTTP response body.
//   *
//   * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
//   * @see [[https://doc.akka.io/docs/akka-http Library documentation]]
//   * @see [[https://doc.akka.io/api/akka-http/current/akka/http/ API]]
//   * @param handler RPC request handler
//   * @param method HTTP method to server
//   * @param mapException maps an exception to a corresponding HTTP status code
//   * @param readTimeout request body read timeout
//   * @tparam Effect effect type
//   * @return Akka HTTP actor behavior
//   */
//  def apply[Effect[_]](
//    handler: Types.HandlerAnyCodec[Effect, Context],
//    materializer: ActorMaterializer,
//    mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
//    readTimeout: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS)
//  ): Behavior[Message] = {
//    Behaviors.receiveMessage { message =>
//      // Log the request
//      val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
//      implicit val system: EffectSystem[Effect] = genericHandler.system
//      val requestId = Random.id
//      val request = message.request
//      lazy val requestProperties = getRequestProperties(request, requestId)
//      log.receivedRequest(requestProperties)
//
//      // Process the request
//      {
//        implicit val streamMaterializer: ActorMaterializer = materializer
//        request.entity.toStrict(readTimeout).map { requestEntity =>
//          val requestBody = Bytes.byteBuffer.from(requestEntity.data.asByteBuffer)
//          genericHandler.processRequest(requestBody, getRequestContext(request), requestId).either.map(_.fold(
//            error => sendErrorResponse(error, message.replyTo, request, requestId, requestProperties),
//            result => {
//              // Send the response
//              val responseBody = result.responseBody.getOrElse(new ArraySeq.ofByte(Array()))
//              val statusCode = result.exception.map(mapException).map(StatusCode.int2StatusCode).getOrElse(StatusCodes.OK)
//              sendResponse(responseBody, statusCode, result.context, message.replyTo, request, requestId)
//            }
//          ))
//        }
//      }
//      Behaviors.same
//    }
//  }
//
//  private def sendErrorResponse(
//    error: Throwable,
//    replyTo: ActorRef[HttpResponse],
//    request: HttpRequest,
//    requestId: String,
//    requestProperties: => Map[String, String]
//  ): Unit = {
//    log.failedProcessRequest(error, requestProperties)
//    val responseBody = Bytes.string.from(error.trace.mkString("\n"))
//    sendResponse(responseBody, StatusCodes.InternalServerError, None, replyTo, request, requestId)
//  }
//
//  private def sendResponse(
//    responseBody: ArraySeq.ofByte,
//    statusCode: StatusCode,
//    responseContext: Option[Context],
//    replyTo: ActorRef[HttpResponse],
//    request: HttpRequest,
//    requestId: String
//  ): Unit = {
//    // Log the response
//    val responseStatusCode = responseContext.flatMap(_.statusCode.map(StatusCode.int2StatusCode)).getOrElse(statusCode)
//    lazy val responseProperties = ListMap(
//      LogProperties.requestId -> requestId,
//      "Client" -> clientAddress(request),
//      "Status" -> responseStatusCode.intValue.toString
//    )
//    log.sendingResponse(responseProperties)
//
//    // Send the response
//    Try {
//      val baseResponse = setResponseContext(HttpResponse(), responseContext)
//      val response = baseResponse
//        .withStatus(responseStatusCode)
//        .withHeaders(
//          response.headers :+ ContentType.parse(genericHandler.protocol.codec.mediaType).swap.map { errors =>
//            new IllegalStateException(s"Invalid response content type: ${errors.map(_.toString).mkString("\n")}")
//          }.swap.toTry.get
//        )
//        .withEntity(HttpEntity(Bytes.byteArray.to(responseBody)))
//      replyTo.tell(response)
//      log.sentResponse(responseProperties)
//    }.onFailure { error =>
//      log.failedSendResponse(error, responseProperties)
//    }.get
//  }
//
//  private def getRequestContext(request: HttpRequest): Context =
//    HttpContext(
//      transport = Some(request),
//      method = Some(HttpMethod.valueOf(request.method.value)),
//      headers = request.headers.map(header => header.name -> header.value)
//    ).url(request.uri.toString)
//
//  private def setResponseContext(response: HttpResponse, responseContext: Option[Context]): HttpResponse =
//    response.withHeaders(responseContext.toSeq.flatMap(_.headers).map { case (name, value) =>
//      HttpHeader(name, value)
//    })
//
//  private def getRequestProperties(request: HttpRequest, requestId: String): Map[String, String] =
//    ListMap(
//      LogProperties.requestId -> requestId,
//      "Client" -> clientAddress(request),
//      "URL" -> request.uri.toString,
//      "Method" -> request.method.value
//    )
//
//  private def clientAddress(request: HttpRequest): String =
//    Network.address(None, "")
//
//  final case class Message(request: HttpRequest, replyTo: ActorRef[HttpResponse])
//
//  final case class HttpHeader(name: String, value: String) extends akka.http.scaladsl.model.HttpHeader {
//
//    override def lowercaseName(): String =
//      name.toLowerCase
//
//    override def renderInRequests(): Boolean =
//      true
//
//    override def renderInResponses(): Boolean =
//      true
//  }
//}
