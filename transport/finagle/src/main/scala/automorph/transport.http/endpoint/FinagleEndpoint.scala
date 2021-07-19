package automorph.transport.http.endpoint

import automorph.Handler
import automorph.handler.HandlerResult
import automorph.log.Logging
import automorph.protocol.{ErrorType, ResponseError}
import automorph.spi.{EndpointMessageTransport, MessageFormat}
import automorph.transport.http.HttpProperties
import automorph.transport.http.endpoint.FinagleEndpoint.{Context, defaultErrorStatus}
import automorph.util.{Bytes, Network}
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
 * @param runEffect asynchronous effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status code mapping function
 * @tparam Effect effect type
 */
final case class FinagleEndpoint[Node, Effect[_]](
  handler: Handler.AnyFormat[Effect, Context],
  runEffect: Effect[Any] => Any,
  errorStatus: Int => Status = defaultErrorStatus
) extends Service[Request, Response] with Logging with EndpointMessageTransport {

  private val system = handler.system

  override def apply(request: Request): Future[Response] = {
    // Receive the request
    val client = clientAddress(request)
    logger.debug("Received HTTP request", Map("Client" -> client, "Size" -> request.content.length))
    val requestMessage = Buf.ByteArray.Owned.extract(request.content)

    // Process the request
    implicit val usingContext: Context= createContext(request)
    runAsFuture(system.map(
      system.either(handler.processRequest(requestMessage)),
      (handlerResult: Either[Throwable, HandlerResult[Array[Byte]]]) =>
        handlerResult.fold(
          error => serverError(error, request),
          result => {
            // Send the response
            val response = result.response.getOrElse(Array[Byte]())
            val status = result.errorCode.map(errorStatus).getOrElse(Status.Ok)
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
    val message = Reader.fromBuf(Buf.Utf8(ResponseError.trace(error).mkString("\n")))
    val status = Status.InternalServerError
    createResponse(message, status, request)
  }

  private def createResponse(message: Reader[Buf], status: Status, request: Request): Response = {
    val response = Response(request.version, status, message)
    response.contentType = handler.format.mediaType
    logger.debug(
      "Sending HTTP response",
      Map("Client" -> clientAddress(request), "Status" -> status.code, "Size" -> response.content.length)
    )
    response
  }

  private def createContext(request: Request): Context =
    HttpProperties(
      source = Some(request),
      method = Some(request.method.name),
      scheme = request.uri,
      path = request.path,
      query = request.uri,
      headers = request.headerMap.iterator.toSeq
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

case object FinagleEndpoint {

  /** Request context type. */
  type Context = HttpProperties[Request]

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus: Int => Status = Map(
    ErrorType.ParseError -> Status.BadRequest,
    ErrorType.InvalidRequest -> Status.BadRequest,
    ErrorType.MethodNotFound -> Status.NotImplemented,
    ErrorType.InvalidParams -> Status.BadRequest,
    ErrorType.InternalError -> Status.InternalServerError,
    ErrorType.IOError -> Status.InternalServerError,
    ErrorType.ApplicationError -> Status.InternalServerError
  ).withDefaultValue(Status.InternalServerError).map { case (errorType, status) =>
    errorType.code -> status
  }
}
