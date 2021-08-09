package automorph.protocol.restrpc

import automorph.protocol.restrpc.RestRpcProtocol.{defaultErrorToException, defaultExceptionToError}
import automorph.spi.Message.Params
import automorph.spi.RpcProtocol.{InvalidRequestException, InvalidResponseException}
import automorph.spi.protocol.{RpcError, RpcMessage, RpcRequest, RpcResponse}
import automorph.spi.{Message, MessageFormat, MessageType, RpcProtocol}
import automorph.util.Extensions.{ThrowableOps, TryOps}
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}

/**
 * REST-RPC protocol implementation.
 *
 * @constructor Creates a REST-RPC 2.0 protocol implementation.
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @param errorToException maps a REST-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding REST-RPC error
 */
final case class RestRpcProtocol(
  errorToException: (String, Option[Int]) => Throwable = defaultErrorToException,
  exceptionToError: Throwable => Option[Int] = defaultExceptionToError
) extends RpcProtocol {

  type Details = RestRpcProtocol.Details

  override val name: String = "REST-RPC"

  override def parseRequest[Node](
    request: ArraySeq.ofByte,
    method: Option[String],
    format: MessageFormat[Node]
  ): Either[RpcError[Details], RpcRequest[Node, Details]] =
//    // Deserialize request
//    Try(format.deserialize(request)).pureFold(
//      error => Left(RpcError(InvalidRequest("Invalid request format", error), RpcMessage(None, request))),
//      formedRequest => {
//        // Validate request
//        val messageText = () => Some(format.text(formedRequest))
//        val message = RpcMessage(formedRequest.id, request, formedRequest.properties, messageText)
//        Try(Request(formedRequest)).pureFold(
//          error => Left(RpcError(error, message)),
//          validRequest => Right(RpcRequest(validRequest.method, validRequest.params, validRequest.id.isDefined, message))
//        )
//      }
//    )
  ???

  override def createResponse[Node](
    result: Try[Node],
    details: Details,
    format: MessageFormat[Node],
    encodeStrings: List[String] => Node
  ): Try[RpcResponse[Node, Details]] = {
    val formedResponse = result.pureFold(
      error => {
        val responseError = error match {
          case RestRpcException(message, code, data, _) =>
            ResponseError(message, code, data.asInstanceOf[Option[Node]])
          case _ =>
            // Assemble error details
            val trace = error.trace
            val message = trace.headOption.getOrElse("Unknown error")
            val code = exceptionToError(error)
            val data = Some(encodeStrings(trace.drop(1).toList))
            ResponseError(message, code, data)
        }
        Response[Node](None, Some(responseError)).formed
      },
      resultValue => Response(Some(resultValue), None).formed
    )
    val messageText = () => Some(format.text(formedResponse))
    Try(format.serialize(formedResponse)).mapFailure { error =>
      InvalidResponseException("Invalid response format", error)
    }.map { messageBody =>
      val message = RpcMessage((), messageBody, formedResponse.properties, messageText)
      RpcResponse(result, message)
    }
  }

  override def createRequest[Node](
    method: String,
    argumentNames: Option[Seq[String]],
    argumentValues: Seq[Node],
    responseRequired: Boolean,
    format: MessageFormat[Node]
  ): Try[RpcRequest[Node, Details]] = {
    val argumentNodes = createArgumentNodes(argumentNames, argumentValues)
//    val formedRequest = Request(id, method, argumentNodes).formed
    val properties = Map(
      "Type" -> MessageType.Call.toString,
      "Method" -> method,
      "Arguments" -> argumentValues.size.toString
    )
//    val messageText = () => format.text(formedRequest)
//    Try(format.serialize(formedRequest)).mapFailure { error =>
//      InvalidRequestException("Invalid request format", error)
//    }.map { messageBody =>
//      val message = RpcMessage(id, messageBody, formedRequest.properties, Some(messageText))
//      RpcRequest(method, argumentNodes, respond, message)
//    }
    ???
  }

  override def parseResponse[Node](
    response: ArraySeq.ofByte,
    format: MessageFormat[Node]
  ): Either[RpcError[Details], RpcResponse[Node, Details]] =
  // Deserialize response
    Try(format.deserialize(response)).pureFold(
      error => Left(RpcError(InvalidResponseException("Invalid response format", error), RpcMessage((), response))),
      formedResponse => {
        // Validate response
        val messageText = () => Some(format.text(formedResponse))
        val message = RpcMessage((), response, formedResponse.properties, messageText)
        Try(Response(formedResponse)).pureFold(
          error => Left(RpcError(InvalidResponseException("Invalid response format", error), message)),
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
   * Creates a copy of this protocol with specified exception to REST-RPC error mapping.
   *
   * @param exceptionToError maps an exception classs to a corresponding REST-RPC error type
   * @return REST-RPC protocol
   */
  def exceptionToError(exceptionToError: Throwable => Option[Int]): RestRpcProtocol =
    copy(exceptionToError = exceptionToError)

  /**
   * Creates a copy of this protocol with specified REST-RPC error to exception mapping.
   *
   * @param errorToException maps a REST-RPC error to a corresponding exception
   * @return REST-RPC protocol
   */
  def errorToException(errorToException: (String, Option[Int]) => Throwable): RestRpcProtocol =
    copy(errorToException = errorToException)

  /**
   * Creates method invocation argument nodes.
   *
   * @param argumentNames argument names
   * @param encodedArguments encoded arguments
   * @return argument nodes
   */
  private def createArgumentNodes[Node](argumentNames: Option[Seq[String]], encodedArguments: Seq[Node]): Params[Node] =
    argumentNames.filter(_.size >= encodedArguments.size).map { names =>
      Right(names.zip(encodedArguments).toMap)
    }.getOrElse(Left(encodedArguments.toList))
}

case object RestRpcProtocol {

  /** REST-RPC request properties. */
  type Details = Unit

  /**
   * Maps a REST-RPC error to a corresponding default exception.
   *
   * @param message error message
   * @param code error code
   * @return exception
   */
  def defaultErrorToException(message: String, code: Option[Int]): Throwable = code match {
    case _ => new RuntimeException(message)
  }

  /**
   * Maps an exception to a corresponding default REST-RPC error type.
   *
   * @param exception exception
   * @return REST-RPC error type
   */
  def defaultExceptionToError(exception: Throwable): Option[Int] = exception match {
    case _ => None
  }
}
