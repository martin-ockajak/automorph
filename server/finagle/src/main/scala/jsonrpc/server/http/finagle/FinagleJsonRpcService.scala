package jsonrpc.server.http.finagle

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.{Promise, Future as TwitterFuture}
import jsonrpc.JsonRpcHandler
import jsonrpc.core.Protocol
import jsonrpc.core.Protocol.ErrorType
import jsonrpc.log.Logging
import jsonrpc.server.http.finagle.FinagleJsonRpcService.defaultStatuses
import jsonrpc.spi.Backend
import jsonrpc.util.EncodingOps.{asArraySeq, toArraySeq}
import scala.collection.immutable.ArraySeq
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * JSON-RPC HTTP service for Finagle RPC system.
 *
 * The endpoint interprets HTTP request body as a JSON-RPC request and processes it using the specified JSON-RPC handler.
 * The response returned by the JSON-RPC handler is used as HTTP response body.
 *
 * @see [[https://finagle.github.io/finch Documentation]]
 * @constructor Create a JSON=RPC HTTP handler for Undertow web server using the specified JSON-RPC ''handler''.
 * @param handler JSON-RPC request handler
 * @param errorStatus JSON-RPC error code to HTTP status code mapping function
 * @param executionContext execution context
 */
final case class FinagleJsonRpcService(
  handler: JsonRpcHandler[?, ?, Future, Request],
  errorStatus: Int => Status = defaultStatuses
)(using executionContext: ExecutionContext)
  extends Service[Request, Response] with Logging:

  private val backend = handler.backend

  override def apply(request: Request): TwitterFuture[Response] =
    // Receive the request
    val client = clientAddress(request)
    logger.debug("Received HTTP request", Map("Client" -> client))
    val rawRequest = Buf.ByteArray.Owned.extract(request.content).asArraySeq

    // Process the request
    asTwitterFuture(backend.map(
      backend.either(handler.processRequest(rawRequest)(using request)),
      _.fold(
        error => serverErrorResponse(error, request),
        result =>
          // Send the response
          val response = result.response.getOrElse(ArraySeq.ofByte(Array.empty))
          val status = result.errorCode.map(errorStatus).getOrElse(Status.Ok)
          val reader = Reader.fromBuf(Buf.ByteArray.Owned(response.unsafeArray))
          logger.debug("Sending HTTP response", Map("Client" -> client, "Status" -> status.code.toString))
          Response(request.version, status, reader)
      )
    ))

  private def serverErrorResponse(error: Throwable, request: Request): Response =
    val status = Status.InternalServerError
    val errorMessage = Protocol.errorDetails(error).mkString("\n")
    val reader = Reader.fromBuf(Buf.Utf8(errorMessage))
    logger.error("Failed processing HTTP request", error, Map("Client" -> clientAddress(request)))
    Response(request.version, status, reader)

  private def clientAddress(request: Request): String =
    val forwardedFor = request.xForwardedFor
    forwardedFor.map(_.split(",", 2)(0)).getOrElse {
      val address = request.remoteAddress.toString.split("/", 2).reverse.head
      address.replaceAll("/", "").split(":").init.mkString(":")
    }

  private def asTwitterFuture[T](future: Future[T]): TwitterFuture[T] =
    val promise = Promise[T]()
    future.onComplete {
      case Success(value)     => promise.setValue(value)
      case Failure(exception) => promise.setException(exception)
    }(executionContext)
    promise

case object FinagleJsonRpcService:

  /** Error propagaring mapping of JSON-RPC error types to HTTP status codes. */
  val defaultStatuses = Map(
    ErrorType.ParseError -> Status.BadRequest,
    ErrorType.InvalidRequest -> Status.BadRequest,
    ErrorType.MethodNotFound -> Status.NotImplemented,
    ErrorType.InvalidParams -> Status.BadRequest,
    ErrorType.InternalError -> Status.InternalServerError,
    ErrorType.IOError -> Status.InternalServerError,
    ErrorType.ApplicationError -> Status.InternalServerError
  ).withDefaultValue(Status.InternalServerError).map((errorType, status) => errorType.code -> status)
