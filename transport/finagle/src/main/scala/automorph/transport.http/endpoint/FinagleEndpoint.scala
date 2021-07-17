package automorph.transport.http.endpoint

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Future, Promise}
import automorph.Handler
import automorph.handler.HandlerResult
import automorph.protocol.ResponseError
import automorph.log.Logging
import automorph.transport.http.endpoint.FinagleEndpoint.defaultErrorStatus
import automorph.spi.{EndpointMessageTransport, MessageFormat}
import automorph.protocol.ErrorType

/**
 * Finagle RPC system endpoint transport plugin using HTTP as message transport protocol.
 *
 * The service interprets HTTP request body as a RPC request and processes it with the specified RPC handler.
 * The response returned by the RPC handler is used as HTTP response body.
 *
 * @see [[https://twitter.github.io/finagle/ Documentation]]
 * @see [[https://twitter.github.io/finagle/docs/com/twitter/finagle/ API]]
 * @constructor Creates a Finagle RPC system RPC service with the specified RPC request ''handler''.
 * @param handler RPC request handler
 * @param runEffect asynchronous effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status code mapping function
 * @tparam Node message node type
 * @tparam Effect effect type
 */
final case class FinagleEndpoint[Node, Effect[_]](
  handler: Handler[Node, _ <: MessageFormat[Node], Effect, Request],
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
    implicit val usingContext: Request = request
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
    val message = Reader.fromBuf(Buf.Utf8(ResponseError.trace(error).mkString("\n")))
    val status = Status.InternalServerError
    logger.error(
      "Failed to process HTTP request",
      error,
      Map("Client" -> clientAddress(request), "Size" -> request.content.length)
    )
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

  private def clientAddress(request: Request): String =
    request.xForwardedFor.flatMap(_.split(",", 2).headOption).getOrElse {
      val address = request.remoteAddress.toString.split("/", 2).reverse.head
      address.replaceAll("/", "").split(":").init.mkString(":")
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
  type Context = Request

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
