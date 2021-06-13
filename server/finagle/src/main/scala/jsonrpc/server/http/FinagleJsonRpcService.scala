package jsonrpc.server.http

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Future, Promise}
import jsonrpc.Handler
import jsonrpc.protocol.Errors
import jsonrpc.log.Logging
import jsonrpc.server.http.FinagleJsonRpcService.defaultErrorStatus
import jsonrpc.protocol.ErrorType.ErrorType
import jsonrpc.spi.Backend
import jsonrpc.util.Encoding.toArraySeq
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}

/**
 * JSON-RPC HTTP service for Finagle RPC system.
 *
 * The service interprets HTTP request body as a JSON-RPC request and processes it using the specified JSON-RPC handler.
 * The response returned by the JSON-RPC handler is used as HTTP response body.
 *
 * @see [[https://finagle.github.io/finch Documentation]]
 * @constructor Create a JSON=RPC HTTP handler for Undertow web server using the specified JSON-RPC request ''handler''.
 * @param handler JSON-RPC request handler
 * @param effectRunAsync asynchronous effect execution function
 * @param errorStatus JSON-RPC error code to HTTP status code mapping function
 * @tparam Effect effect type
 */
final case class FinagleJsonRpcService[Effect[_]](
  handler: Handler[?, ?, Effect, Request],
  errorStatus: Int => Status = defaultErrorStatus
)
  extends Service[Request, Response] with Logging:

  private val backend = handler.backend

  override def apply(request: Request): Future[Response] =
    // Receive the request
    val client = clientAddress(request)
    logger.debug("Received HTTP request", Map("Client" -> client))
    val rawRequest = ArraySeq.ofByte(Buf.ByteArray.Owned.extract(request.content))

    // Process the request
    asFuture(backend.map(
      backend.either(handler.processRequest(rawRequest)(using request)),
      _.fold(
        error => serverError(error, request),
        result =>
          // Send the response
          val response = result.response.getOrElse(ArraySeq.ofByte(Array.empty))
          val status = result.errorCode.map(errorStatus).getOrElse(Status.Ok)
          val reader = Reader.fromBuf(Buf.ByteArray.Owned(response.unsafeArray))
          createResponse(request, reader, status)
      )
    ))

  private def serverError(error: Throwable, request: Request): Response =
    val status = Status.InternalServerError
    val errorMessage = Errors.errorDetails(error).mkString("\n")
    val reader = Reader.fromBuf(Buf.Utf8(errorMessage))
    logger.error("Failed to process HTTP request", error, Map("Client" -> clientAddress(request)))
    createResponse(request, reader, status)

  private def createResponse(request: Request, reader: Reader[Buf], status: Status): Response =
    val response = Response(request.version, status, reader)
    response.contentType = handler.codec.mediaType
    logger.debug("Sending HTTP response", Map("Client" -> clientAddress(request), "Status" -> status.code.toString))
    response

  private def clientAddress(request: Request): String =
    request.xForwardedFor.map(_.split(",", 2)(0)).getOrElse {
      val address = request.remoteAddress.toString.split("/", 2).reverse.head
      address.replaceAll("/", "").split(":").init.mkString(":")
    }

  private def asFuture[T](value: Effect[T]): Future[T] =
    val promise = Promise[T]()
    backend.map(backend.either(value), _.fold(
      error => promise.setException(error),
      result => promise.setValue(result)
    ))
    promise

case object FinagleJsonRpcService:

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultErrorStatus = Map(
    ErrorType.ParseError -> Status.BadRequest,
    ErrorType.InvalidRequest -> Status.BadRequest,
    ErrorType.MethodNotFound -> Status.NotImplemented,
    ErrorType.InvalidParams -> Status.BadRequest,
    ErrorType.InternalError -> Status.InternalServerError,
    ErrorType.IOError -> Status.InternalServerError,
    ErrorType.ApplicationError -> Status.InternalServerError
  ).withDefaultValue(Status.InternalServerError).map((errorType, status) => errorType.code -> status)
