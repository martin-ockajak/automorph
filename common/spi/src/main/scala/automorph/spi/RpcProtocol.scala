package automorph.spi

import automorph.spi.protocol.{RpcError, RpcFunction, RpcRequest, RpcResponse}
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * Remote procedure call (RPC) protocol plugin.
 *
 * The underlying RPC protocol must support remote function invocation.
 *
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 * @tparam Context message context type
 */
trait RpcProtocol[Node, Codec <: MessageCodec[Node], Context] {

  /** RPC message body type. */
  type MessageBody = ArraySeq.ofByte

  /** Protocol-specific RPC message metadata. */
  type Metadata

  /** Protocol name. */
  val name: String

  /** Message codec plugin. */
  val codec: Codec

  /**
   * Creates an RPC request.
   *
   * @param functionName invoked function name
   * @param argumentNames argument names
   * @param argumentValues argument values
   * @param responseRequired true if the request mandates a response, false if there should be no response
   * @param requestId request correlation identifier
   * @return RPC request
   */
  def createRequest(
    functionName: String,
    argumentNames: Option[Iterable[String]],
    argumentValues: Iterable[Node],
    responseRequired: Boolean,
    requestId: String
  ): Try[RpcRequest[Node, Metadata]]

  /**
   * Parses an RPC request.
   *
   * @param requestBody RPC request message body
   * @param requestContext request context
   * @param requestId request correlation identifier
   * @return RPC request if the message is valid or RPC error if the message is invalid
   */
  def parseRequest(
    requestBody: MessageBody,
    requestContext: Context,
    requestId: String
  ): Either[RpcError[Metadata], RpcRequest[Node, Metadata]]

  /**
   * Creates an RPC response.
   *
   * @param result RPC response result
   * @param requestMetadata corresponding RPC request metadata
   * @return RPC response
   */
  def createResponse(result: Try[Node], requestMetadata: Metadata): Try[RpcResponse[Node, Metadata]]

  /**
   * Parses an RPC response.
   *
   * @param responseBody RPC response message body
   * @param responseContext response context
   * @return RPC response if the message is valid or RPC error if the message is invalid
   */
  def parseResponse(
    responseBody: MessageBody,
    responseContext: Context
  ): Either[RpcError[Metadata], RpcResponse[Node, Metadata]]

  /**
   * Generates OpenApi speficication for specified RPC API functions.
   *
   * @see https://github.com/OAI/OpenAPI-Specification
   * @param functions RPC API functions
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URLs
   * @return OpenAPI specification
   */
  def openApi(functions: Iterable[RpcFunction], title: String, version: String, serverUrls: Iterable[String]): String
}

object RpcProtocol {

  /** Invalid RPC request error. */
  final case class InvalidRequestException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)

  /** Invalid RPC response error. */
  final case class InvalidResponseException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)

  /** Remote function not found error. */
  final case class FunctionNotFoundException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)
}
