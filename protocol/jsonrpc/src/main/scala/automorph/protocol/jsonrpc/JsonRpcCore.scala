package automorph.protocol.jsonrpc

import automorph.protocol.JsonRpcProtocol
import automorph.protocol.jsonrpc.ErrorType.ParseErrorException
import automorph.protocol.jsonrpc.Message.Params
import automorph.spi.RpcProtocol.InvalidResponseException
import automorph.spi.protocol.{RpcError, RpcMessage, RpcRequest, RpcResponse}
import automorph.spi.MessageCodec
import automorph.util.Extensions.{ThrowableOps, TryOps}
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Random, Success, Try}

/**
 * JSON-RPC protocol core logic.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
private[automorph] trait JsonRpcCore[Node, Codec <: MessageCodec[Node]] {
  this: JsonRpcProtocol[Node, Codec] =>

  /** JSON-RPC request details. */
  type Details = Option[Message.Id]

  private val unknownId = Right("[unknown]")
  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  override val name: String = "JSON-RPC"

  override def parseRequest(
    request: ArraySeq.ofByte,
    function: Option[String]
  ): Either[RpcError[Details], RpcRequest[Node, Details]] =
    // Deserialize request
    Try(decodeMessage(codec.deserialize(request))).pureFold(
      error => Left(RpcError(ParseErrorException("Malformed request", error), RpcMessage(None, request))),
      formedRequest => {
        // Validate request
        val messageText = () => Some(codec.text(encodeMessage(formedRequest)))
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
    responseRequired: Boolean
  ): Try[RpcRequest[Node, Details]] = {
    // Create request
    Seq(function)
    val id = Option.when(responseRequired)(Right(Math.abs(random.nextLong()).toString).withLeft[BigDecimal])
    val argumentNodes = createArgumentNodes(argumentNames, argumentValues)
    val formedRequest = Request(id, function, argumentNodes).formed

    // Serialize request
    val messageText = () => Some(codec.text(encodeMessage(formedRequest)))
    Try(codec.serialize(encodeMessage(formedRequest))).mapFailure { error =>
      ParseErrorException("Malformed request", error)
    }.map { messageBody =>
      val message = RpcMessage(id, messageBody, formedRequest.properties, messageText)
      RpcRequest(function, argumentNodes, responseRequired, message)
    }
  }

  override def createResponse(
    result: Try[Node],
    details: Details
  ): Try[RpcResponse[Node, Details]] = {
    // Create response
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

    // Serialize response
    val messageText = () => Some(codec.text(encodeMessage(formedResponse)))
    Try(codec.serialize(encodeMessage(formedResponse))).mapFailure { error =>
      ParseErrorException("Malformed response", error)
    }.map { messageBody =>
      val message = RpcMessage(Option(id), messageBody, formedResponse.properties, messageText)
      RpcResponse(result, message)
    }
  }

  override def parseResponse(response: ArraySeq.ofByte): Either[RpcError[Details], RpcResponse[Node, Details]] =
  // Deserialize response
    Try(decodeMessage(codec.deserialize(response))).pureFold(
      error => Left(RpcError(ParseErrorException("Malformed response", error), RpcMessage(None, response))),
      formedResponse => {
        // Validate response
        val messageText = () => Some(codec.text(encodeMessage(formedResponse)))
        val message = RpcMessage(formedResponse.id, response, formedResponse.properties, messageText)
        Try(Response(formedResponse)).pureFold(
          error => Left(RpcError(ParseErrorException("Malformed response", error), message)),
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
