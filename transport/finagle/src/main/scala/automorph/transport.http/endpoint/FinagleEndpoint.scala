package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.spi.transport.EndpointMessageTransport
import automorph.transport.http.Http
import automorph.transport.http.endpoint.FinagleEndpoint.Context
import automorph.util.Extensions.ThrowableOps
import automorph.util.Network
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Future, Promise}

/**
 * Finagle RPC system endpoint transport plugin using HTTP as message transport protocol.
 *
 * The service interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://twitter.github.io/finagle/ Documentation]]
 * @see [[https://twitter.github.io/finagle/docs/com/twitter/finagle/ API]]
 * @constructor Creates a Finagle RPC system HTTP service with the specified RPC request ''handler''.
 * @param handler RPC request handler
 * @param runEffect executes specified effect asynchronously
 * @param exceptionToStatusCode maps an exception to a corresponding HTTP status code
 * @tparam Effect effect type
 */
final case class FinagleEndpoint[Effect[_]](
  handler: Handler.AnyCodec[Effect, Context],
  runEffect: Effect[Any] => Unit,
  exceptionToStatusCode: Throwable => Int = Http.defaultExceptionToStatusCode
) extends Service[Request, Response] with Logging with EndpointMessageTransport {

  private val system = handler.system

  override def apply(request: Request): Future[Response] = {
    // Receive the request
    val client = clientAddress(request)
    logger.debug("Received HTTP request", Map("Client" -> client, "Size" -> request.content.length))
    val requestMessage = Buf.ByteArray.Owned.extract(request.content)

    // Process the request
    implicit val usingContext: Context = createContext(request)
    runAsFuture(system.map(
      system.either(handler.processRequest(requestMessage)),
      (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
        handlerResult.fold(
          error => serverError(error, request),
          result => {
            // Send the response
            val response = result.response.getOrElse(Array[Byte]())
            val status = result.exception.map(exceptionToStatusCode).map(Status.apply).getOrElse(Status.Ok)
            val message = Reader.fromBuf(Buf.ByteArray.Owned(response))
            createResponse(message, status, request)
          }
        )
    ))
  }

  private def serverError(error: Throwable, request: Request): Response = {
    logger.error(
      "Failed to process HTTP request",
      error,
      Map("Client" -> clientAddress(request), "Size" -> request.content.length)
    )
    val message = Reader.fromBuf(Buf.Utf8(error.trace.mkString("\n")))
    val status = Status.InternalServerError
    createResponse(message, status, request)
  }

  private def createResponse(message: Reader[Buf], status: Status, request: Request): Response = {
    val response = Response(request.version, status, message)
    response.contentType = handler.codec.mediaType
    logger.debug(
      "Sending HTTP response",
      Map("Client" -> clientAddress(request), "Status" -> status.code, "Size" -> response.content.length)
    )
    response
  }

  private def createContext(request: Request): Context = Http(
    source = Some(request),
    method = Some(request.method.name),
    headers = request.headerMap.iterator.toSeq
  ).url(request.uri)

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
