package automorph.protocol

import automorph.protocol.JsonRpcProtocol.{ParseErrorException, defaultErrorToException, defaultExceptionToError}
import automorph.protocol.jsonrpc.{ErrorType, JsonRpcException, Request, Response, ResponseError}
import automorph.spi.Message.Params
import automorph.spi.RpcProtocol.{FunctionNotFoundException, InvalidRequestException, InvalidResponseException}
import automorph.spi.protocol.{RpcError, RpcMessage, RpcRequest, RpcResponse}
import automorph.spi.{Message, MessageCodec, RpcProtocol}
import automorph.util.Extensions.{ThrowableOps, TryOps}
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Random, Success, Try}

/**
 * JSON-RPC 2.0 protocol implementation.
 *
 * @constructor Creates a JSON-RPC 2.0 protocol implementation.
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @param codec message codec plugin
 * @param errorToException maps a JSON-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding JSON-RPC error
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class JsonRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  errorToException: (String, Int) => Throwable = defaultErrorToException,
  exceptionToError: Throwable => ErrorType = defaultExceptionToError
) extends RpcProtocol[Node] {

  type Details = JsonRpcProtocol.Details
  type Helper = Unit

  private val unknownId = Right("[unknown]")
  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  override val name: String = "JSON-RPC"

  override def parseRequest(
    request: ArraySeq.ofByte,
    function: Option[String]
  ): Either[RpcError[Details], RpcRequest[Node, Details]] =
    // Deserialize request
    Try(codec.deserialize(request)).pureFold(
      error => Left(RpcError(ParseErrorException("Invalid request codec", error), RpcMessage(None, request))),
      formedRequest => {
        // Validate request
        val messageText = () => Some(codec.text(formedRequest))
        val message = RpcMessage(formedRequest.id, request, formedRequest.properties, messageText)
        Try(Request(formedRequest)).pureFold(
          error => Left(RpcError(error, message)),
          validRequest => Right(RpcRequest(validRequest.method, validRequest.params, validRequest.id.isDefined, message))
        )
      }
    )

  override def createRequest(
    function: String,
    argumentNames: Option[Seq[String]],
    argumentValues: Seq[Node],
    responseRequest: Boolean
  ): Try[RpcRequest[Node, Details]] = {
    val id = Option.when(responseRequest)(Right(Math.abs(random.nextLong()).toString).withLeft[BigDecimal])
    val argumentNodes = createArgumentNodes(argumentNames, argumentValues)
    val formedRequest = Request(id, function, argumentNodes).formed
    val messageText = () => Some(codec.text(formedRequest))
    Try(codec.serialize(formedRequest)).mapFailure { error =>
      ParseErrorException("Invalid request codec", error)
    }.map { messageBody =>
      val message = RpcMessage(id, messageBody, formedRequest.properties, messageText)
      RpcRequest(function, argumentNodes, responseRequest, message)
    }
  }

  override def createResponse(
    result: Try[Node],
    details: Details,
    encodeStrings: List[String] => Node
  ): Try[RpcResponse[Node, Details]] = {
    val id = details.getOrElse(unknownId)
    val formedResponse = result.pureFold(
      error => {
        val responseError = error match {
          case JsonRpcException(message, code, data, _) =>
            ResponseError(message, code, data.asInstanceOf[Option[Node]])
          case _ =>
            // Assemble error details
            val trace = error.trace
            val message = trace.headOption.getOrElse("Unknown error")
            val code = exceptionToError(error).code
            val data = Some(encodeStrings(trace.drop(1).toList))
            ResponseError(message, code, data)
        }
        Response[Node](id, None, Some(responseError)).formed
      },
      resultValue => Response(id, Some(resultValue), None).formed
    )
    val messageText = () => Some(codec.text(formedResponse))
    Try(codec.serialize(formedResponse)).mapFailure { error =>
      ParseErrorException("Invalid response codec", error)
    }.map { messageBody =>
      val message = RpcMessage(Option(id), messageBody, formedResponse.properties, messageText)
      RpcResponse(result, message)
    }
  }

  override def parseResponse(response: ArraySeq.ofByte): Either[RpcError[Details], RpcResponse[Node, Details]] =
    // Deserialize response
    Try(codec.deserialize(response)).pureFold(
      error => Left(RpcError(ParseErrorException("Invalid response codec", error), RpcMessage(None, response))),
      formedResponse => {
        // Validate response
        val messageText = () => Some(codec.text(formedResponse))
        val message = RpcMessage(formedResponse.id, response, formedResponse.properties, messageText)
        Try(Response(formedResponse)).pureFold(
          error => Left(RpcError(ParseErrorException("Invalid response codec", error), message)),
          validResponse =>
            // Check for error
            validResponse.error.fold(
              // Check for result
              validResponse.result match {
                case None => Left(RpcError(InvalidResponseException("Invalid result", None.orNull), message))
                case Some(result) => Right(RpcResponse(Success(result), message))
              }
            ) { error =>
              Right(RpcResponse(Failure(errorToException(error.message, error.code)), message))
            }
        )
      }
    )

  /**
   * Creates a copy of this protocol with specified exception to JSON-RPC error mapping.
   *
   * @param exceptionToError maps an exception classs to a corresponding JSON-RPC error type
   * @return JSON-RPC protocol
   */
  def exceptionToError(exceptionToError: Throwable => ErrorType): JsonRpcProtocol[Node, Codec] =
    copy(exceptionToError = exceptionToError)

  /**
   * Creates a copy of this protocol with specified JSON-RPC error to exception mapping.
   *
   * @param errorToException maps a JSON-RPC error to a corresponding exception
   * @return JSON-RPC protocol
   */
  def errorToException(errorToException: (String, Int) => Throwable): JsonRpcProtocol[Node, Codec] =
    copy(errorToException = errorToException)

  /**
   * Creates function invocation argument nodes.
   *
   * @param argumentNames argument names
   * @param encodedArguments encoded arguments
   * @return argument nodes
   */
  private def createArgumentNodes(argumentNames: Option[Seq[String]], encodedArguments: Seq[Node]): Params[Node] =
    argumentNames.filter(_.size >= encodedArguments.size).map { names =>
      Right(names.zip(encodedArguments).toMap)
    }.getOrElse(Left(encodedArguments.toList))
}

case object JsonRpcProtocol {

  /** JSON-RPC request properties. */
  type Details = Option[Message.Id]

  /** JSON-RPC parse error. */
  final case class ParseErrorException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)

  /** JSON-RPC internal error. */
  final case class InternalErrorException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)

  /** JSON-RPC sever error. */
  final case class ServerErrorException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)

  /**
   * Maps a JSON-RPC error to a corresponding default exception.
   *
   * @param message error message
   * @param code error code
   * @return exception
   */
  def defaultErrorToException(message: String, code: Int): Throwable = code match {
    case ErrorType.ParseError.code => ParseErrorException(message)
    case ErrorType.InvalidRequest.code => InvalidRequestException(message)
    case ErrorType.MethodNotFound.code => FunctionNotFoundException(message)
    case ErrorType.InvalidParams.code => new IllegalArgumentException(message)
    case ErrorType.InternalError.code => InternalErrorException(message)
    case _ if Range(ErrorType.ReservedError.code, ErrorType.ServerError.code + 1).contains(code) =>
      ServerErrorException(message)
    case _ => new RuntimeException(message)
  }

  /**
   * Maps an exception to a corresponding default JSON-RPC error type.
   *
   * @param exception exception
   * @return JSON-RPC error type
   */
  def defaultExceptionToError(exception: Throwable): ErrorType = exception match {
    case _: ParseErrorException => ErrorType.ParseError
    case _: InvalidRequestException => ErrorType.InvalidRequest
    case _: FunctionNotFoundException => ErrorType.MethodNotFound
    case _: IllegalArgumentException => ErrorType.InvalidParams
    case _: InternalErrorException => ErrorType.InternalError
    case _: ServerErrorException => ErrorType.ServerError
    case _ => ErrorType.ApplicationError
  }
}
