package automorph.spi

import automorph.spi.MessageCodec
import automorph.spi.protocol.{RpcError, RpcRequest, RpcResponse}
import scala.collection.immutable.ArraySeq
import scala.util.Try

/**
 * RPC protocol.
 *
 * @tparam Node message node type
 */
trait RpcProtocol[Node] {

  /** Protocol-specific message details type. */
  type Details

  /** Protocol-specific helper type. */
  type Helper

  /**
   * Protocol name.
   *
   * @return protocol name
   */
  def name: String

  /**
   * Creates an RPC request.
   *
   * @param method method name
   * @param argumentNames argument names
   * @param argumentValues argument values
   * @param responseRequired true if the request mandates a response, false if there should be no response
   * @return RPC request
   */
  def createRequest(
    method: String,
    argumentNames: Option[Seq[String]],
    argumentValues: Seq[Node],
    responseRequired: Boolean
  ): Try[RpcRequest[Node, Details]]

  /**
   * Parses an RPC request.
   *
   * @param request RPC request message
   * @param method method name override, if specified it is used instead of method name obtained from the request
   * @return RPC request on valid request message or RPC error on invalid request message
   */
  def parseRequest(request: ArraySeq.ofByte, method: Option[String]): Either[RpcError[Details], RpcRequest[Node, Details]]

  /**
   * Creates an RPC response.
   *
   * @param result RPC response result
   * @param details corresponding RPC request details
   * @param encodeStrings converts list of strings to message codec node
   * @return RPC response
   */
  def createResponse(
    result: Try[Node],
    details: Details,
    encodeStrings: List[String] => Node
  ): Try[RpcResponse[Node, Details]]

  /**
   * Parses an RPC response.
   *
   * @param response RPC response message
   * @return RPC response on valid response message or RPC error on invalid response message
   */
  def parseResponse(response: ArraySeq.ofByte): Either[RpcError[Details], RpcResponse[Node, Details]]
}

case object RpcProtocol {

  /** Invalid request error. */
  final case class InvalidRequestException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)

  /** Invalid response error. */
  final case class InvalidResponseException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)

  /** JSON-RPC method not found error. */
  final case class MethodNotFoundException(
    message: String,
    cause: Throwable = None.orNull
  ) extends RuntimeException(message, cause)
}
