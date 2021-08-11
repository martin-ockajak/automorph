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

  /** REST-RPC request details. */
  type Details = Unit

  private val errorSchema: Schema = Schema(
    Some(OpenApi.objectType),
    Some(OpenApi.errorTitle),
    Some(s"$name${OpenApi.errorTitle}"),
    Some(Map(
      "error" -> Schema(
        Some("string"),
        Some("error"),
        Some("Failed function call error details"),
        Some(Map(
          "code" -> Schema(Some("integer"), Some("code"), Some("Error code")),
          "message" -> Schema(Some("string"), Some("message"), Some("Error message")),
          "data" -> Schema(Some("object"), Some("data"), Some("Additional error information"))
        )),
        Some(List("code", "message"))
      )
    )),
    Some(List("error"))
  )

  override val name: String = "REST-RPC"

  override def parseRequest(
    request: ArraySeq.ofByte,
    function: Option[String]
  ): Either[RpcError[Details], RpcRequest[Node, Details]] =
    // Deserialize request
    Try(decodeRequest(codec.deserialize(request))).pureFold(
      error => Left(RpcError(InvalidRequestException("Malformed request", error), RpcMessage((), request))),
      formedRequest => {
        // Validate request
        val messageText = () => Some(codec.text(encodeRequest(formedRequest)))
        val requestProperties = Map(
          "Type" -> MessageType.Call.toString,
          "Arguments" -> formedRequest.size.toString
        )
        function.map { functionName =>
          val message = RpcMessage((), request, requestProperties ++ Option("Function" -> functionName), messageText)
          Right(RpcRequest(functionName, Right(formedRequest), true, message))
        }.getOrElse {
          val message = RpcMessage((), request, requestProperties, messageText)
          Left(RpcError(InvalidRequestException("Missing invoked function name"), message))
        }
      }
    )

  override def createResponse(result: Try[Node], details: Details): Try[RpcResponse[Node, Details]] = {
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

  override def createRequest(
    function: String,
    argumentNames: Option[Seq[String]],
    argumentValues: Seq[Node],
    responseRequired: Boolean
  ): Try[RpcRequest[Node, Details]] =
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
        RpcRequest(function, Right(formedRequest), responseRequired, message)
      }
    }

  override def parseResponse(response: ArraySeq.ofByte): Either[RpcError[Details], RpcResponse[Node, Details]] =
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
  private def createArgumentNodes(argumentNames: Option[Seq[String]], encodedArguments: Seq[Node]): Try[Request[Node]] =
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
    Some(s"$name${OpenApi.resultTitle}"),
    Some(Map("result" -> OpenApi.resultSchema(function))),
    Some(List("result"))
  )
}
