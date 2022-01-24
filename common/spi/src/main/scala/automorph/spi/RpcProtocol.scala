package automorph.spi

import automorph.spi.protocol.{RpcApiSchema, RpcError, RpcRequest, RpcResponse}
import java.io.InputStream
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

  /** Protocol-specific RPC message metadata. */
  type Metadata

  /** Protocol name. */
  def name: String

  /** Message codec plugin. */
  val codec: Codec

  /**
   * Creates an RPC request.
   *
   * @param functionName invoked function name
   * @param arguments named arguments
   * @param responseRequired true if the request mandates a response, false if there should be no response
   * @param requestId request correlation identifier
   * @return RPC request
   */
  def createRequest(
    functionName: String,
    arguments: Iterable[(String, Node)],
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
    requestBody: InputStream,
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
    responseBody: InputStream,
    responseContext: Context
  ): Either[RpcError[Metadata], RpcResponse[Node, Metadata]]

  /** RPC API schema operations. */
  def apiSchemas: Seq[RpcApiSchema[Node]]
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
