package automorph.transport.http.endpoint

import automorph.Types
import automorph.handler.HandlerResult
import automorph.log.{LogProperties, Logging}
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.endpoint.FinagleEndpoint.Context
import automorph.util.Extensions.ThrowableOps
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
 * @param runEffect executes specified effect asynchronously
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class FinagleEndpoint[Effect[_]](
  handler: Types.HandlerAnyCodec[Effect, Context],
  runEffect: Effect[Any] => Unit,
  exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
) extends Service[Request, Response] with Logging with EndpointMessageTransport {

  private val genericHandler = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]]
  private val system = genericHandler.system

  override def apply(request: Request): Future[Response] = {
    // Receive the request
    val requestId = Random.id
    lazy val requestDetails = requestProperties(request, requestId)
    logger.debug("Received HTTP request", requestDetails)
    val requestMessage = Buf.ByteArray.Owned.extract(request.content)

    // Process the request
    implicit val usingContext: Context = createContext(request)
    runAsFuture(system.map(
      system.either(genericHandler.processRequest(requestMessage, requestId, Some(request.path))),
      (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
        handlerResult.fold(
          error => serverError(error, request, requestId, requestDetails),
          result => {
            // Send the response
            val response = result.responseBody.getOrElse(Array[Byte]())
            val status = result.exception.map(exceptionToStatusCode).map(Status.apply).getOrElse(Status.Ok)
            val message = Reader.fromBuf(Buf.ByteArray.Owned(response))
            createResponse(message, status, request, requestId)
          }
        )
    ))
  }

  private def serverError(
    error: Throwable,
    request: Request,
    requestId: String,
    requestDetails: => Map[String, String]
  ): Response = {
    logger.error("Failed to process HTTP request", error, requestDetails)
    val message = Reader.fromBuf(Buf.Utf8(error.trace.mkString("\n")))
    val status = Status.InternalServerError
    createResponse(message, status, request, requestId)
  }

  private def createResponse(message: Reader[Buf], status: Status, request: Request, requestId: String): Response = {
    lazy val responseDetails = Map(
      LogProperties.requestId -> requestId,
      "Client" -> clientAddress(request),
      "Status" -> status.toString
    )
    val response = Response(request.version, status, message)
    response.contentType = genericHandler.protocol.codec.mediaType
    logger.debug("Sending HTTP response", responseDetails)
    response
  }

  private def createContext(request: Request): Context = Http(
    base = Some(request),
    method = Some(request.method.name),
    headers = request.headerMap.iterator.toSeq
  ).url(request.uri)

  private def requestProperties(
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
    runEffect(system.map(
      system.either(value),
      (outcome: Either[Throwable, T]) =>
        outcome.fold(
          error => promise.setException(error),
          result => promise.setValue(result)
        )
    ))
    promise
  }
}

object FinagleEndpoint {

  /** Request context type. */
  type Context = Http[Request]
}
