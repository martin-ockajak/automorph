package automorph.protocol.jsonrpc

import automorph.openapi.{OpenApi, RpcSchema, Schema}
import automorph.protocol.JsonRpcProtocol
import automorph.protocol.jsonrpc.ErrorType.ParseErrorException
import automorph.protocol.jsonrpc.Message.Params
import automorph.spi.MessageCodec
import automorph.spi.RpcProtocol.InvalidResponseException
import automorph.spi.protocol.{RpcError, RpcFunction, RpcMessage, RpcRequest, RpcResponse}
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

  /** JSON-RPC message metadata. */
  type Metadata = Option[Message.Id]

  private val unknownId = Right("[unknown]")

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
          "data" -> Schema(Some("object"), Some("data"), Some("Additional error information"))
        )),
        Some(List("message"))
      )
    )),
    Some(List("error"))
  )
  private lazy val random = new Random(System.currentTimeMillis() + Runtime.getRuntime.totalMemory())

  override val name: String = "JSON-RPC"

  override def parseRequest(
    request: ArraySeq.ofByte,
    function: Option[String]
  ): Either[RpcError[Metadata], RpcRequest[Node, Metadata]] =
    // Deserialize request
    Try(decodeMessage(codec.deserialize(request))).pureFold(
      error => Left(RpcError(ParseErrorException("Malformed request", error), RpcMessage(None, request))),
      formedRequest => {
        // Validate request
        Seq(function)
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
    argumentNames: Option[Iterable[String]],
    argumentValues: Iterable[Node],
    responseRequired: Boolean
  ): Try[RpcRequest[Node, Metadata]] = {
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

  override def createResponse(result: Try[Node], details: Metadata): Try[RpcResponse[Node, Metadata]] = {
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

  override def parseResponse(response: ArraySeq.ofByte): Either[RpcError[Metadata], RpcResponse[Node, Metadata]] =
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
  private def createArgumentNodes(
    argumentNames: Option[Iterable[String]],
    encodedArguments: Iterable[Node]
  ): Params[Node] =
    argumentNames.filter(_.size >= encodedArguments.size).map { names =>
      Right(names.zip(encodedArguments).toMap)
    }.getOrElse(Left(encodedArguments.toList))

  private def requestSchema(function: RpcFunction): Schema = Schema(
    Some(OpenApi.objectType),
    Some(OpenApi.requestTitle),
    Some(s"$name ${OpenApi.requestTitle}"),
    Some(Map(
      "jsonrpc" -> Schema(Some("string"), Some("jsonrpc"), Some("Protocol version (must be 2.0)")),
      "function" -> Schema(Some("string"), Some("function"), Some("Invoked function name")),
      "params" -> Schema(
        Some(OpenApi.objectType),
        Some(function.name),
        Some(OpenApi.argumentsDescription),
        OpenApi.maybe(OpenApi.parameterSchemas(function)),
        OpenApi.maybe(OpenApi.requiredParameters(function))
      ),
      "id" -> Schema(
        Some("integer"),
        Some("id"),
        Some("Call identifier, a request without and identifier is considered to be a notification")
      )
    )),
    Some(List("jsonrpc", "function", "params"))
  )

  private def resultSchema(function: RpcFunction): Schema = Schema(
    Some(OpenApi.objectType),
    Some(OpenApi.resultTitle),
    Some(s"$name ${OpenApi.resultTitle}"),
    Some(Map("result" -> OpenApi.resultSchema(function))),
    Some(List("result"))
  )
}
