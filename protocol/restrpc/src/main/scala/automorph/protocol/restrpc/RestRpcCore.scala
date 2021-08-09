package automorph.protocol.restrpc

import automorph.protocol.RestRpcProtocol
import automorph.protocol.restrpc.{Response, ResponseError, RestRpcException}
import automorph.spi.Message.Params
import automorph.spi.RpcProtocol.InvalidResponseException
import automorph.spi.protocol.{RpcError, RpcMessage, RpcRequest, RpcResponse}
import automorph.spi.{MessageCodec, MessageType}
import automorph.util.Extensions.{ThrowableOps, TryOps}
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}

/**
 * REST-RPC protocol core logic.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
private[automorph] trait RestRpcCore[Node, Codec <: MessageCodec[Node]] {
  this: RestRpcProtocol[Node, Codec] =>

  /** REST-RPC request details. */
  type Details = Unit

  override val name: String = "REST-RPC"

  override def parseRequest(
    request: ArraySeq.ofByte,
    function: Option[String]
  ): Either[RpcError[Details], RpcRequest[Node, Details]] =
  //    // Deserialize request
  //    Try(codec.deserialize(request)).pureFold(
  //      error => Left(RpcError(InvalidRequest("Invalid request codec", error), RpcMessage(None, request))),
  //      formedRequest => {
  //        // Validate request
  //        val messageText = () => Some(codec.text(formedRequest))
  //        val message = RpcMessage(formedRequest.id, request, formedRequest.properties, messageText)
  //        Try(Request(formedRequest)).pureFold(
  //          error => Left(RpcError(error, message)),
  //          validRequest => Right(RpcRequest(validRequest.method, validRequest.params, validRequest.id.isDefined, message))
  //        )
  //      }
  //    )
    ???

  override def createResponse(
    result: Try[Node],
    details: Details,
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
    val messageText = () => Some(codec.text(formedResponse))
    Try(codec.serialize(formedResponse)).mapFailure { error =>
      InvalidResponseException("Invalid response codec", error)
    }.map { messageBody =>
      val message = RpcMessage((), messageBody, formedResponse.properties, messageText)
      RpcResponse(result, message)
    }
  }

  override def createRequest(
    function: String,
    argumentNames: Option[Seq[String]],
    argumentValues: Seq[Node],
    responseRequired: Boolean
  ): Try[RpcRequest[Node, Details]] = {
    val argumentNodes = createArgumentNodes(argumentNames, argumentValues)
    //    val formedRequest = Request(id, function, argumentNodes).formed
    val properties = Map(
      "Type" -> MessageType.Call.toString,
      "Method" -> function,
      "Arguments" -> argumentValues.size.toString
    )
    //    val messageText = () => codec.text(formedRequest)
    //    Try(codec.serialize(formedRequest)).mapFailure { error =>
    //      InvalidRequestException("Invalid request codec", error)
    //    }.map { messageBody =>
    //      val message = RpcMessage(id, messageBody, formedRequest.properties, Some(messageText))
    //      RpcRequest(function, argumentNodes, respond, message)
    //    }
    ???
  }

  override def parseResponse(response: ArraySeq.ofByte): Either[RpcError[Details], RpcResponse[Node, Details]] =
  // Deserialize response
    Try(codec.deserialize(response)).pureFold(
      error => Left(RpcError(InvalidResponseException("Invalid response codec", error), RpcMessage((), response))),
      formedResponse => {
        // Validate response
        val messageText = () => Some(codec.text(formedResponse))
        val message = RpcMessage((), response, formedResponse.properties, messageText)
        Try(Response(formedResponse)).pureFold(
          error => Left(RpcError(InvalidResponseException("Invalid response codec", error), message)),
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
  def exceptionToError(exceptionToError: Throwable => Option[Int]): RestRpcProtocol[Node, Codec] =
    copy(exceptionToError = exceptionToError)

  /**
   * Creates a copy of this protocol with specified REST-RPC error to exception mapping.
   *
   * @param errorToException maps a REST-RPC error to a corresponding exception
   * @return REST-RPC protocol
   */
  def errorToException(errorToException: (String, Option[Int]) => Throwable): RestRpcProtocol[Node, Codec] =
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
