package automorph.spi

import automorph.protocol.{RpcError, RpcRequest, RpcResponse}
import automorph.spi.MessageFormat
import scala.collection.immutable.ArraySeq
import scala.util.Try

/** RPC protocol. */
trait Protocol {

  /** Protocol-specific message content type. */
  type Content

  /**
   * Protocol name.
   *
   * @return protocol name
   */
  def name: String

  /**
   * Parses an RPC request.
   *
   * @param request RPC request message
   * @param format message format plugin
   * @param method method name override, if specified it is used instead of method name obtained from the request
   * @tparam Node message node type
   * @return RPC request on valid request message or RPC error on invalid request message
   */
  def parseRequest[Node](
    request: ArraySeq.ofByte,
    format: MessageFormat[Node],
    method: Option[String]
  ): Either[RpcError[Content], RpcRequest[Node, Content]]

  /**
   * Parses an RPC response.
   *
   * @param response RPC response message
   * @param format message format plugin
   * @tparam Node message node type
   * @return RPC response on valid response message or RPC error on invalid response message
   */
  def parseResponse[Node](
    response: ArraySeq.ofByte,
    format: MessageFormat[Node]
  ): Either[RpcError[Content], RpcResponse[Node, Content]]

  /**
   * Creates an RPC request.
   *
   * @param method method name
   * @param argumentNames argument names
   * @param argumentValues argument values
   * @param respond true if the request mandates a response
   * @param format message format plugin
   * @tparam Node message node type
   * @return RPC request
   */
  def createRequest[Node](
    method: String,
    argumentNames: Option[Seq[String]],
    argumentValues: Seq[Node],
    respond: Boolean,
    format: MessageFormat[Node]
  ): Try[RpcRequest[Node, Content]]

  /**
   * Creates an RPC response.
   *
   * @param result RPC response result
   * @param content corresponding RPC request content
   * @param format message format plugin
   * @param encodeStrings converts list of strings to message format node
   * @tparam Node message node type
   * @return RPC response
   */
  def createResponse[Node](
    result: Try[Node],
    content: Content,
    format: MessageFormat[Node],
    encodeStrings: List[String] => Node
  ): Try[RpcResponse[Node, Content]]
}

case object Protocol {

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
