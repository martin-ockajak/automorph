package automorph.spi

import automorph.spi.protocol.{RpcApiSchema, RpcError, RpcRequest, RpcResponse}
import java.io.InputStream
import scala.util.Try

/**
 * Remote procedure call protocol plugin.
 *
 * The underlying RPC protocol must support remote function invocation.
 *
 * @tparam Node
 *   message node type
 * @tparam Codec
 *   message codec plugin type
 * @tparam Context
 *   message context type
 */
trait RpcProtocol[Node, Codec <: MessageCodec[Node], Context] {

  /** Protocol-specific RPC message metadata. */
  type Metadata

  /** Message codec plugin. */
  val messageCodec: Codec

  /** Protocol name. */
  def name: String

  /**
   * Creates an RPC request.
   *
   * @param function
   *   invoked function name
   * @param arguments
   *   named arguments
   * @param responseRequired
   *   true if the request mandates a response, false if there should be no response
   * @param requestContext
   * request context
   * @param requestId
   *   request correlation identifier
   * @return
   *   RPC request
   */
  def createRequest(
    function: String,
    arguments: Iterable[(String, Node)],
    responseRequired: Boolean,
    requestContext: Context,
    requestId: String,
  ): Try[RpcRequest[Node, Metadata, Context]]

  /**
   * Parses an RPC request.
   *
   * @param requestBody
   *   RPC request message body
   * @param requestContext
   *   request context
   * @param requestId
   *   request correlation identifier
   * @return
   *   RPC request if the message is valid or RPC error if the message is invalid
   */
  def parseRequest(
    requestBody: InputStream,
    requestContext: Context,
    requestId: String,
  ): Either[RpcError[Metadata], RpcRequest[Node, Metadata, Context]]

  /**
   * Creates an RPC response.
   *
   * @param result
   *   RPC response result
   * @param requestMetadata
   *   corresponding RPC request metadata
   * @return
   *   RPC response
   */
  def createResponse(result: Try[Node], requestMetadata: Metadata): Try[RpcResponse[Node, Metadata]]

  /**
   * Parses an RPC response.
   *
   * @param responseBody
   *   RPC response message body
   * @param responseContext
   *   response context
   * @return
   *   RPC response if the message is valid or RPC error if the message is invalid
   */
  def parseResponse(
    responseBody: InputStream,
    responseContext: Context,
  ): Either[RpcError[Metadata], RpcResponse[Node, Metadata]]

  /** RPC API schema operations. */
  def apiSchemas: Seq[RpcApiSchema[Node]]
}
