package automorph.transport.http.endpoint

import automorph.Types
import automorph.log.{LogProperties, Logging}
import automorph.spi.EffectSystem
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.HttpContext
import automorph.transport.http.endpoint.FinagleEndpoint.{Context, Run}
import automorph.util.Extensions.{EffectOps, ThrowableOps}
import automorph.util.{Network, Random}
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Future, Promise}

/**
 * Finagle HTTP endpoint message transport plugin.
 *
 * The service interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://en.wikipedia.org/wiki/Hypertext Transport protocol]]
 * @see [[https://twitter.github.io/finagle Library documentation]]
 * @see [[https://twitter.github.io/finagle/docs/com/twitter/finagle/ API]]
 * @constructor Creates a Finagle HTTP service with the specified RPC request handler.
 * @param handler RPC request handler
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @param runEffect executes specified effect asynchronously
 * @tparam Effect effect type
 */
final case class FinagleEndpoint[Effect[_]] private (
  handler: Types.HandlerAnyCodec[Effect, Context],
  exceptionToStatusCode: Throwable => Int,
  runEffect: Run[Effect]
) extends Service[Request, Response] with Logging with EndpointMessageTransport {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system
  implicit private val givenSystem: EffectSystem[Effect] = system

  override def apply(request: Request): Future[Response] = {
    // Log the request
    val requestId = Random.id
    lazy val requestProperties = extractRequestProperties(request, requestId)
    logger.debug("Received HTTP request", requestProperties)
    val requestBody = Buf.ByteArray.Owned.extract(request.content)

    // Process the request
    implicit val givenContext: Context = requestContext(request)
    runAsFuture(genericHandler.processRequest(requestBody, requestId, Some(request.path)).either.map(_.fold(
      error => sendErrorResponse(error, request, requestId, requestProperties),
      result => {
        // Send the response
        val response = result.responseBody.getOrElse(Array[Byte]())
        val status = result.exception.map(exceptionToStatusCode).map(Status.apply).getOrElse(Status.Ok)
        val message = Reader.fromBuf(Buf.ByteArray.Owned(response))
        createResponse(message, status, result.context, request, requestId)
      }
    )))
  }

  private def sendErrorResponse(
    error: Throwable,
    request: Request,
    requestId: String,
    requestProperties: => Map[String, String]
  ): Response = {
    logger.error("Failed to process HTTP request", error, requestProperties)
    val message = Reader.fromBuf(Buf.Utf8(error.trace.mkString("\n")))
    createResponse(message, Status.InternalServerError, None, request, requestId)
  }

  private def createResponse(
    message: Reader[Buf],
    status: Status,
    responseContext: Option[Context],
    request: Request,
    requestId: String
  ): Response = {
    // Log the response
    val responseStatus = responseContext.flatMap(_.statusCode.map(Status.apply)).getOrElse(status)
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "Status" -> responseStatus.toString
    )

    // Send the response
    // FIXME - set headers from response context
    val response = Response(request.version, responseStatus, message)
    response.contentType = genericHandler.protocol.codec.mediaType
    logger.debug("Sending HTTP response", responseDetails)
    response
  }

  private def requestContext(request: Request): Context = HttpContext(
    base = Some(request),
    method = Some(request.method.name),
    headers = request.headerMap.iterator.toSeq
  ).url(request.uri)

  private def extractRequestProperties(
    request: Request,
    requestId: String
  ): Map[String, String] = Map(
    LogProperties.requestId -> requestId,
    "Client" -> clientAddress(request),
    "URL" -> request.uri,
    "Method" -> request.method.toString
  )

  private def clientAddress(request: Request): String = {
    val forwardedFor = request.xForwardedFor
    val address = request.remoteAddress.toString
    Network.address(forwardedFor, address)
  }

  private def runAsFuture[T](value: Effect[T]): Future[T] = {
    val promise = Promise[T]()
    runEffect(value.either.map(_.fold(
      error => promise.setException(error),
      result => promise.setValue(result)
    )))
    promise
  }
}

object FinagleEndpoint {

  /**
   * Asynchronous effect execution function type.
   *
   * @tparam Effect effect type
   */
  type Run[Effect[_]] = Effect[Any] => Unit

  /** Request context type. */
  type Context = HttpContext[Request]

  /**
   * Creates a Finagle HTTP endpoint message transport plugin with the specified RPC request handler.
   *
   * Resulting function requires:
   * - effect execution function - executes specified effect asynchronously
   *
   * @param handler RPC request handler
   * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
   * @tparam Effect effect type
   * @return creates an Finagle HTTP service using supplied asynchronous effect execution function
   */
  def create[Effect[_]](
    handler: Types.HandlerAnyCodec[Effect, Context],
    exceptionToStatusCode: Throwable => Int = HttpContext.defaultExceptionToStatusCode
  ): (Run[Effect]) => FinagleEndpoint[Effect] = (runEffect: Run[Effect]) =>
    FinagleEndpoint(handler, exceptionToStatusCode, runEffect)
}
