package automorph.protocol.restrpc

import automorph.protocol.Protocol.{InvalidRequestException, InvalidResponseException, MethodNotFoundException}
import automorph.protocol.restrpc.RestRpcProtocol.{defaultErrorToException, defaultExceptionToError}
import automorph.protocol.{Protocol, RpcError, RpcMessage, RpcRequest, RpcResponse}
import automorph.spi.Message.Params
import automorph.spi.{Message, MessageFormat, MessageType}
import automorph.util.Extensions.TryOps
import automorph.util.MessageId
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
) extends Protocol {

  type Content = RestRpcProtocol.Content

  override val name: String = "REST-RPC"

  override def parseRequest[Node](
    request: ArraySeq.ofByte,
    format: MessageFormat[Node],
    method: Option[String]
  ): Either[RpcError[Content], RpcRequest[Node, Content]] =
//    // Deserialize request
//    Try(format.deserialize(request)).pureFold(
//      error => Left(RpcError(ParseErrorException("Invalid request format", error), RpcMessage(None, request))),
//      formedRequest => {
//        // Validate request
//        val messageText = Some(() => format.format(formedRequest))
//        val message = RpcMessage(formedRequest.id, request, formedRequest.properties, messageText)
//        Try(Request(formedRequest)).pureFold(
//          error => Left(RpcError(error, message)),
//          validRequest => Right(RpcRequest(validRequest.method, validRequest.params, validRequest.id.isDefined, message))
//        )
//      }
//    )
  ???

  override def parseResponse[Node](
    response: ArraySeq.ofByte,
    format: MessageFormat[Node]
  ): Either[RpcError[Content], RpcResponse[Node, Content]] =
    // Deserialize response
    Try(format.deserialize(response)).pureFold(
      error => Left(RpcError(InvalidResponseException("Invalid response format", error), RpcMessage((), response))),
      formedResponse => {
        // Validate response
        val messageText = Some(() => format.format(formedResponse))
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

  override def createRequest[Node](
    method: String,
    argumentNames: Option[Seq[String]],
    argumentValues: Seq[Node],
    respond: Boolean,
    format: MessageFormat[Node]
  ): Try[RpcRequest[Node, Content]] = {
    val argumentNodes = createArgumentNodes(argumentNames, argumentValues)
//    val formedRequest = Request(id, method, argumentNodes).formed
    val properties = Map(
      "Type" -> MessageType.Call.toString,
      "Method" -> method,
      "Arguments" -> argumentValues.size.toString
    )
//    val messageText = () => format.format(formedRequest)
//    Try(format.serialize(formedRequest)).mapFailure { error =>
//      ParseErrorException("Invalid request format", error)
//    }.map { messageBody =>
//      val message = RpcMessage(id, messageBody, formedRequest.properties, Some(messageText))
//      RpcRequest(method, argumentNodes, respond, message)
//    }
    ???
  }

  override def createResponse[Node](
    result: Try[Node],
    content: Content,
    format: MessageFormat[Node],
    encodeStrings: List[String] => Node
  ): Try[RpcResponse[Node, Content]] = {
    val formedResponse = result.pureFold(
      error => {
        val responseError = error match {
          case RestRpcException(message, code, data, _) =>
            ResponseError(message, code, data.asInstanceOf[Option[Node]])
          case _ =>
            // Assemble error details
            val trace = Protocol.trace(error)
            val message = trace.headOption.getOrElse("Unknown error")
            val code = exceptionToError(error)
            val data = Some(encodeStrings(trace.drop(1).toList))
            ResponseError(message, code, data)
        }
        Response[Node](None, Some(responseError)).formed
      },
      resultValue => Response(Some(resultValue), None).formed
    )
    val messageText = () => format.format(formedResponse)
    Try(format.serialize(formedResponse)).mapFailure { error =>
      InvalidResponseException("Invalid response format", error)
    }.map { messageBody =>
      val message = RpcMessage((), messageBody, formedResponse.properties, Some(messageText))
      RpcResponse(result, message)
    }
  }

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
  type Content = Unit

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