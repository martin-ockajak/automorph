package automorph.spi

import automorph.spi.MessageCodec
import automorph.spi.protocol.{RpcError, RpcRequest, RpcResponse}
import scala.collection.immutable.ArraySeq
import scala.util.Try

/** RPC protocol. */
trait RpcProtocol {

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
   * @param method method name
   * @param argumentNames argument names
   * @param argumentValues argument values
   * @param responseRequired true if the request mandates a response, false if there should be no response
   * @param codec message codec plugin
   * @tparam Node message node type
   * @return RPC request
   */
  def createRequest[Node](
    method: String,
    argumentNames: Option[Seq[String]],
    argumentValues: Seq[Node],
    responseRequired: Boolean,
    codec: MessageCodec[Node]
  ): Try[RpcRequest[Node, Details]]

  /**
   * Parses an RPC request.
   *
   * @param request RPC request message
   * @param method method name override, if specified it is used instead of method name obtained from the request
   * @param codec message codec plugin
   * @tparam Node message node type
   * @return RPC request on valid request message or RPC error on invalid request message
   */
  def parseRequest[Node](
    request: ArraySeq.ofByte,
    method: Option[String],
    codec: MessageCodec[Node]
  ): Either[RpcError[Details], RpcRequest[Node, Details]]

  /**
   * Creates an RPC response.
   *
   * @param result RPC response result
   * @param details corresponding RPC request details
   * @param codec message codec plugin
   * @param encodeStrings converts list of strings to message codec node
   * @tparam Node message node type
   * @return RPC response
   */
  def createResponse[Node](
    result: Try[Node],
    details: Details,
    codec: MessageCodec[Node],
    encodeStrings: List[String] => Node
  ): Try[RpcResponse[Node, Details]]

  /**
   * Parses an RPC response.
   *
   * @param response RPC response message
   * @param codec message codec plugin
   * @tparam Node message node type
   * @return RPC response on valid response message or RPC error on invalid response message
   */
  def parseResponse[Node](
    response: ArraySeq.ofByte,
    codec: MessageCodec[Node]
  ): Either[RpcError[Details], RpcResponse[Node, Details]]
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
