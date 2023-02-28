package automorph.transport.http.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging, MessageLog}
import automorph.spi.{EffectSystem, EndpointMessageTransport}
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
 * The service interprets HTTP request body as a RPC request and processes it with the specified RPC handler. The
 * response returned by the RPC handler is used as HTTP response body.
 *
 * @see
 *   [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see
 *   [[https://twitter.github.io/finagle Library documentation]]
 * @see
 *   [[https://twitter.github.io/finagle/docs/com/twitter/finagle/ API]]
 * @constructor
 *   Creates a Finagle HTTP service with the specified RPC request handler.
 * @param handler
 *   RPC request handler
 * @param mapException
 *   maps an exception to a corresponding HTTP status code
 * @tparam Effect
 *   effect type
 */
final case class FinagleHttpEndpoint[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  mapException: Throwable => Int = HttpContext.defaultExceptionToStatusCode,
) extends Service[Request, Response] with Logging with EndpointMessageTransport {

  private val log = MessageLog(logger, Protocol.Http.name)
  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  implicit private val system: EffectSystem[Effect] = genericHandler.system

  override def apply(request: Request): Future[Response] = {
    // Log the request
    val requestId = Random.id
    lazy val requestProperties = getRequestProperties(request, requestId)
    log.receivedRequest(requestProperties)
    val requestBody = Buf.ByteArray.Owned.extract(request.content).toInputStream

    // Process the request
    runAsFuture(genericHandler.processRequest(requestBody, getRequestContext(request), requestId).either.map(
      _.fold(
        error => sendErrorResponse(error, request, requestId, requestProperties),
        result => {
          // Send the response
          val responseBody = Reader.fromBuf(Buf.ByteArray.Owned(result.responseBody.map(_.toArray).getOrElse(Array())))
          val status = result.exception.map(mapException).map(Status.apply).getOrElse(Status.Ok)
          createResponse(responseBody, status, result.context, request, requestId)
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
    response.contentType = genericHandler.protocol.codec.mediaType
    log.sendingResponse(responseProperties)
    response
  }

  private def getRequestContext(request: Request): Context =
    HttpContext(
      transport = Some(request),
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
