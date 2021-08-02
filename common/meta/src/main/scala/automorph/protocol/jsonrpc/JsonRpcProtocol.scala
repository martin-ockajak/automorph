package automorph.protocol.jsonrpc

import automorph.log.Logging
import automorph.protocol.Protocol.{InvalidRequestException, InvalidResponseException, MethodNotFoundException}
import automorph.protocol.jsonrpc.ErrorType.{InternalErrorException, ParseErrorException}
import automorph.protocol.jsonrpc.JsonRpcProtocol.{RequestProperties, defaultErrorToException, defaultExceptionToError}
import automorph.protocol.{Protocol, RpcRequest}
import automorph.spi.Message.Params
import automorph.spi.{Message, MessageFormat}
import automorph.util.Extensions.TryOps
import automorph.util.MessageId
import java.io.IOException
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}

final case class JsonRpcProtocol(
  errorToException: (Int, String) => Throwable = defaultErrorToException,
  exceptionToError: Throwable => ErrorType = defaultExceptionToError
) extends Protocol[RequestProperties] with Logging {

  override def parseRequest[Node](
    request: ArraySeq.ofByte,
    method: Option[String],
    format: MessageFormat[Node]
  ): Try[RpcRequest[Node, RequestProperties]] = {
    // Deserialize request
    Try(format.deserialize(request)).mapFailure { error =>
      ParseErrorException("Invalid request format", error)
    }.flatMap { formedRequest =>
      // Validate request
      logger.trace(s"Received JSON-RPC request:\n${format.format(formedRequest)}")
      Try(Request(formedRequest)).map { validRequest =>
        RpcRequest(validRequest.method, validRequest.params, validRequest.id)
      }
    }
  }

  override def parseResponse[Node](response: ArraySeq.ofByte, format: MessageFormat[Node]): Try[Node] = {
    // Deserialize response
    Try(format.deserialize(response)).mapFailure { error =>
      ParseErrorException("Invalid response format", error)
    }.flatMap { formedResponse =>
      // Validate response
      logger.trace(s"Received JSON-RPC response:\n${format.format(formedResponse)}")
      Try(Response(formedResponse)).flatMap { validResponse =>
        // Check for error
        validResponse.error.fold(
          // Check for result
          validResponse.result match {
            case None => Failure(InvalidResponseException("Invalid result", None.orNull))
            case Some(result) => Success(result)
          }
        ) { error =>
          Failure[Node](errorToException(error.code, error.message))
        }
      }
    }
  }

  override def createRequest[Node](
    method: String,
    argumentNames: Option[Seq[String]],
    arguments: Seq[Node],
    format: MessageFormat[Node]
  ): Try[(ArraySeq.ofByte, RpcRequest[Node, RequestProperties])] = {
    val id = Some(Right[BigDecimal, String](MessageId.next))
    val argumentNodes = createArgumentNodes(argumentNames, arguments)
    val formedRequest = Request(id, method, argumentNodes).formed
    logger.trace(s"Sending JSON-RPC request:\n${format.format(formedRequest)}")
    val rpcRequest = RpcRequest[Node, RequestProperties](method, argumentNodes, id)
    Try(format.serialize(formedRequest) -> rpcRequest).mapFailure { error =>
      ParseErrorException("Invalid request format", error)
    }
  }

  override def createResponse[Node](
    result: Try[Node],
    properties: RequestProperties,
    format: MessageFormat[Node],
    encodeStrings: List[String] => Node
  ): Try[ArraySeq.ofByte] =
    properties match {
      case None => Failure(InternalErrorException("Missing response identifier", None.orNull))
      case Some(id) =>
        val formedResponse = result.pureFold(
          error => {
            val responseError = error match {
              case JsonRpcException(message, code, data, _) =>
                ResponseError(message, code, data.asInstanceOf[Option[Node]])
              case _ =>
                // Assemble error details
                val trace = Protocol.trace(error)
                val message = trace.headOption.getOrElse("Unknown error")
                val code = exceptionToError(error).code
                val data = Some(encodeStrings(trace.drop(1).toList))
                ResponseError(message, code, data)
            }
            Response[Node](id, None, Some(responseError)).formed
          },
          resultValue => Response(id, Some(resultValue), None).formed
        )
        logger.trace(s"Sending JSON-RPC response:\n${format.format(formedResponse)}")
        Try(format.serialize(formedResponse)).mapFailure { error =>
          ParseErrorException("Invalid response format", error)
        }
    }

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

case object JsonRpcProtocol {

  /** JSON-RPC request properties. */
  type RequestProperties = Option[Message.Id]

  /**
   * Maps a JSON-RPC error to a corresponding default exception.
   *
   * @param code error code
   * @param message error message
   * @return exception
   */
  def defaultErrorToException(code: Int, message: String): Throwable = code match {
    case ErrorType.ParseError.code => ParseErrorException(message, None.orNull)
    case ErrorType.InvalidRequest.code => InvalidRequestException(message, None.orNull)
    case ErrorType.MethodNotFound.code => MethodNotFoundException(message, None.orNull)
    case ErrorType.InvalidParams.code => new IllegalArgumentException(message, None.orNull)
    case ErrorType.InternalError.code => InternalErrorException(message, None.orNull)
    case ErrorType.IOError.code => new IOException(message, None.orNull)
    case _ if code < ErrorType.ApplicationError.code => InternalErrorException(message, None.orNull)
    case _ => new RuntimeException(message, None.orNull)
  }

  /**
   * Maps an exception class to a corresponding default JSON-RPC error type.
   *
   * @param exception exception class
   * @return JSON-RPC error type
   */
  def defaultExceptionToError(exception: Throwable): ErrorType = exception match {
    case _: ParseErrorException => ErrorType.ParseError
    case _: InvalidRequestException => ErrorType.InvalidRequest
    case _: MethodNotFoundException => ErrorType.MethodNotFound
    case _: IllegalArgumentException => ErrorType.InvalidParams
    case _: InternalErrorException => ErrorType.InternalError
    case _: IOException => ErrorType.IOError
    case _ => ErrorType.ApplicationError
  }
}
