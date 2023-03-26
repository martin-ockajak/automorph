package automorph.transport.http.endpoint

import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointTransport, RequestHandler}
import automorph.transport.http.endpoint.FinagleHttpEndpoint.Context
import automorph.transport.http.{HttpContext, HttpMethod, Protocol}
import automorph.util.Extensions.{ByteArrayOps, EffectOps, InputStreamOps, ThrowableOps}
import automorph.util.{Network, Random}
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Future, Promise}
import scala.collection.immutable.ListMap

/**
 * Finagle HTTP endpoint message transport plugin.
 *
 * Interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
 * - The response returned by the RPC handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://twitter.github.io/finagle Library documentation]]
 * @see
 *   [[https://twitter.github.io/finagle/docs/com/twitter/finagle/ API]]
 * @constructor
 *   Creates an Finagle HTTP endpoint message transport plugin with specified effect system and request handler.
 * @param effectSystem
 *   effect system plugin
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @param handler
 *   RPC request handler
 * @tparam Effect
 *   effect type
 */
final case class FinagleHttpEndpoint[Effect[_]](
  effectSystem: EffectSystem[Effect],
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
  handler: RequestHandler[Effect, Context] = RequestHandler.dummy,
) extends Service[Request, Response] with Logging with EndpointTransport[Effect, Context, Service[Request, Response]] {

  private val log = MessageLog(logger, Protocol.Http.name)
  implicit private val system: EffectSystem[Effect] = effectSystem

  override def adapter: Service[Request, Response] =
    this

  override def clone(handler: RequestHandler[Effect, Context]): FinagleHttpEndpoint[Effect] =
    copy(handler = handler)

  override def apply(request: Request): Future[Response] = {
    // Log the request
    val requestId = Random.id
    lazy val requestProperties = getRequestProperties(request, requestId)
    log.receivedRequest(requestProperties)
    val requestBody = Buf.ByteArray.Owned.extract(request.content).toInputStream

    // Process the request
    runAsFuture(handler.processRequest(requestBody, getRequestContext(request), requestId).either.map(
      _.fold(
        error => sendErrorResponse(error, request, requestId, requestProperties),
        result => {
          // Send the response
          val responseBody = Reader.fromBuf(Buf.ByteArray.Owned(result.map(_.responseBody.toArray).getOrElse(Array())))
          val status = result.flatMap(_.exception).map(mapException).map(Status.apply).getOrElse(Status.Ok)
          createResponse(responseBody, status, result.flatMap(_.context), request, requestId)
        },
      )
    ))
  }

  private def sendErrorResponse(
    error: Throwable,
    request: Request,
    requestId: String,
    requestProperties: => Map[String, String],
  ): Response = {
    log.failedProcessRequest(error, requestProperties)
    val responseBody = Reader.fromBuf(Buf.Utf8(error.trace.mkString("\n")))
    createResponse(responseBody, Status.InternalServerError, None, request, requestId)
  }

  private def createResponse(
    responseBody: Reader[Buf],
    status: Status,
    responseContext: Option[Context],
    request: Request,
    requestId: String,
  ): Response = {
    // Log the response
    val responseStatus = responseContext.flatMap(_.statusCode.map(Status.apply)).getOrElse(status)
    lazy val responseProperties = ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "Status" -> responseStatus.toString,
    )

    // Send the response
    val response = Response(request.version, responseStatus, responseBody)
    setResponseContext(response, responseContext)
    response.contentType = handler.mediaType
    log.sendingResponse(responseProperties)
    response
  }

  private def getRequestContext(request: Request): Context =
    HttpContext(
      message = Some(request),
      method = Some(HttpMethod.valueOf(request.method.name)),
      headers = request.headerMap.iterator.toSeq,
    ).url(request.uri)

  private def setResponseContext(response: Response, responseContext: Option[Context]): Unit =
    responseContext.toSeq.flatMap(_.headers).foreach { case (name, value) => response.headerMap.add(name, value) }

  private def getRequestProperties(request: Request, requestId: String): Map[String, String] =
    ListMap(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "URL" -> request.uri,
      "Method" -> request.method.toString,
    )

  private def clientAddress(request: Request): String = {
    val forwardedFor = request.xForwardedFor
    val address = request.remoteAddress.toString
    Network.address(forwardedFor, address)
  }

  private def runAsFuture[T](value: Effect[T]): Future[T] = {
    val promise = Promise[T]()
    value.either.map(_.fold(error => promise.setException(error), result => promise.setValue(result))).runAsync
    promise
  }
}

object FinagleHttpEndpoint {

  /** Request context type. */
  type Context = HttpContext[Request]
}
