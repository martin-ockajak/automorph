package automorph.protocol.restrpc

import automorph.openapi.{OpenApi, RpcSchema, Schema}
import automorph.protocol.RestRpcProtocol
import automorph.protocol.restrpc.Message.Request
import automorph.protocol.restrpc.{Response, ResponseError, RestRpcException}
import automorph.spi.MessageCodec
import automorph.spi.RpcProtocol.{InvalidRequestException, InvalidResponseException}
import automorph.spi.protocol.{RpcError, RpcFunction, RpcMessage, RpcRequest, RpcResponse}
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

  /** REST-RPC message metadata. */
  type Metadata = Unit

  private lazy val errorSchema: Schema = Schema(
    Some(OpenApi.objectType),
    Some(OpenApi.errorTitle),
    Some(s"$name ${OpenApi.errorTitle}"),
    Some(Map(
      "error" -> Schema(
        Some("string"),
        Some("error"),
        Some("Failed function call error details"),
        Some(Map(
          "code" -> Schema(Some("integer"), Some("code"), Some("Error code")),
          "message" -> Schema(Some("string"), Some("message"), Some("Error message")),
          "details" -> Schema(Some("object"), Some("details"), Some("Additional error information"))
        )),
        Some(List("code", "message"))
      )
    )),
    Some(List("error"))
  )

  override val name: String = "REST-RPC"

  override def createRequest(
    function: String,
    argumentNames: Option[Iterable[String]],
    argumentValues: Iterable[Node],
    responseRequired: Boolean,
    requestId: String
  ): Try[RpcRequest[Node, Metadata]] =
  // Create request
    createArgumentNodes(argumentNames, argumentValues).flatMap { formedRequest =>
      val requestProperties = Map(
        "Type" -> MessageType.Call.toString,
        "Function" -> function,
        "Arguments" -> argumentValues.size.toString
      )

      // Serialize request
      val messageText = () => Some(codec.text(encodeRequest(formedRequest)))
      Try(codec.serialize(encodeRequest(formedRequest))).mapFailure { error =>
        InvalidRequestException("Malformed request", error)
      }.map { messageBody =>
        val message = RpcMessage((), messageBody, requestProperties, messageText)
        RpcRequest(message, function, Right(formedRequest), responseRequired, requestId)
      }
    }

  override def parseRequest(
    requestBody: MessageBody,
    requestId: String,
    functionName: Option[String]
  ): Either[RpcError[Metadata], RpcRequest[Node, Metadata]] =
    // Deserialize request
    Try(decodeRequest(codec.deserialize(requestBody))).pureFold(
      error => Left(RpcError(InvalidRequestException("Malformed request", error), RpcMessage((), requestBody))),
      formedRequest => {
        // Validate request
        val messageText = () => Some(codec.text(encodeRequest(formedRequest)))
        val requestProperties = Map(
          "Type" -> MessageType.Call.toString,
          "Arguments" -> formedRequest.size.toString
        )
        functionName.map { functionName =>
          val message = RpcMessage((), requestBody, requestProperties ++ Option("Function" -> functionName), messageText)
          Right(RpcRequest(message, functionName, Right(formedRequest), true, requestId))
        }.getOrElse {
          val message = RpcMessage((), requestBody, requestProperties, messageText)
          Left(RpcError(InvalidRequestException("Missing invoked function name"), message))
        }
      }
    )

  override def createResponse(result: Try[Node], details: Metadata): Try[RpcResponse[Node, Metadata]] = {
    // Create response
    Seq(details)
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

    // Serialize response
    val messageText = () => Some(codec.text(encodeResponse(formedResponse)))
    Try(codec.serialize(encodeResponse(formedResponse))).mapFailure { error =>
      InvalidResponseException("Malformed response", error)
    }.map { messageBody =>
      val message = RpcMessage((), messageBody, formedResponse.properties, messageText)
      RpcResponse(result, message)
    }
  }

  override def parseResponse(response: MessageBody): Either[RpcError[Metadata], RpcResponse[Node, Metadata]] =
    // Deserialize response
    Try(decodeResponse(codec.deserialize(response))).pureFold(
      error => Left(RpcError(InvalidResponseException("Malformed response", error), RpcMessage((), response))),
      formedResponse => {
        // Validate response
        val messageText = () => Some(codec.text(encodeResponse(formedResponse)))
        val message = RpcMessage((), response, formedResponse.properties, messageText)
        Try(Response(formedResponse)).pureFold(
          error => Left(RpcError(InvalidResponseException("Malformed response", error), message)),
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

  override def openApi(
    functions: Iterable[RpcFunction],
    title: String,
    version: String,
    serverUrls: Iterable[String]
  ): String = {
    val functionSchemas = functions.map { function =>
      function -> RpcSchema(requestSchema(function), resultSchema(function), errorSchema)
    }
    OpenApi.specification(functionSchemas, title, version, serverUrls).json
  }

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
  private def createArgumentNodes(
    argumentNames: Option[Iterable[String]],
    encodedArguments: Iterable[Node]
  ): Try[Request[Node]] =
    argumentNames.filter(_.size >= encodedArguments.size).map { names =>
      Success(names.zip(encodedArguments).toMap)
    }.getOrElse {
      Failure(InvalidRequestException("Missing REST-RPC request argument names"))
    }

  private def requestSchema(function: RpcFunction): Schema = Schema(
    Some(OpenApi.objectType),
    Some(function.name),
    Some(OpenApi.argumentsDescription),
    OpenApi.maybe(OpenApi.parameterSchemas(function))
  )

  private def resultSchema(function: RpcFunction): Schema = Schema(
    Some(OpenApi.objectType),
    Some(OpenApi.resultTitle),
    Some(s"$name ${OpenApi.resultTitle}"),
    Some(Map("result" -> OpenApi.resultSchema(function))),
    Some(List("result"))
  )
}
