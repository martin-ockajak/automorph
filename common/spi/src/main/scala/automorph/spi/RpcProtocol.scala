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
 */
trait RpcProtocol[Node] {

  /** Protocol-specific message details type. */
  type Details

  /**
   * Protocol name.
   *
   * @return protocol name
   */
  def name: String

  /**
   * Creates an RPC request.
   *
   * @param function function name
   * @param argumentNames argument names
   * @param argumentValues argument values
   * @param responseRequired true if the request mandates a response, false if there should be no response
   * @return RPC request
   */
  def createRequest(
    function: String,
    argumentNames: Option[Iterable[String]],
    argumentValues: Iterable[Node],
    responseRequired: Boolean
  ): Try[RpcRequest[Node, Details]]

  /**
   * Parses an RPC request.
   *
   * @param request RPC request message
   * @param function function name override, if specified it is used instead of function name obtained from the request
   * @return RPC request on valid request message or RPC error on invalid request message
   */
  def parseRequest(
    request: ArraySeq.ofByte,
    function: Option[String]
  ): Either[RpcError[Details], RpcRequest[Node, Details]]

  /**
   * Creates an RPC response.
   *
   * @param result RPC response result
   * @param details corresponding RPC request details
   * @return RPC response
   */
  def createResponse(
    result: Try[Node],
    details: Details
  ): Try[RpcResponse[Node, Details]]

  /**
   * Parses an RPC response.
   *
   * @param response RPC response message
   * @return RPC response on valid response message or RPC error on invalid response message
   */
  def parseResponse(response: ArraySeq.ofByte): Either[RpcError[Details], RpcResponse[Node, Details]]

  /**
   * Generates OpenApi speficication for specified RPC functions.
   *
   * @see https://github.com/OAI/OpenAPI-Specification
   * @param functions API functions
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URLs
   * @return OpenAPI specification
   */
  def openApi(functions: Iterable[RpcFunction], title: String, version: String, serverUrls: Iterable[String]): String
}

case object RpcProtocol {

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
