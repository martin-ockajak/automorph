package automorph.protocol

import automorph.protocol.RestRpcProtocol.{defaultErrorToException, defaultExceptionToError}
import automorph.protocol.restrpc.{ErrorMapping, Message, RestRpcCore}
import automorph.spi.{MessageCodec, RpcProtocol}

/**
 * REST-RPC protocol implementation.
 *
 * @constructor Creates a REST-RPC 2.0 protocol implementation.
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @param codec message codec plugin
 * @param errorToException maps a REST-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding REST-RPC error
 * @param encodeRequest converts a REST-RPC request to message format node
 * @param decodeRequest converts a message format node to REST-RPC request
 * @param encodeResponse converts a REST-RPC response to message format node
 * @param decodeResponse converts a message format node to REST-RPC response
 * @param encodeStrings converts list of strings to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class RestRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  errorToException: (String, Option[Int]) => Throwable,
  exceptionToError: Throwable => Option[Int],
  protected val encodeRequest: Message.Request[Node] => Node,
  protected val decodeRequest: Node => Message.Request[Node],
  protected val encodeResponse: Message[Node] => Node,
  protected val decodeResponse: Node => Message[Node],
  protected val encodeStrings: List[String] => Node
) extends RestRpcCore[Node, Codec] with RpcProtocol[Node, Codec]

object RestRpcProtocol extends ErrorMapping:

  /**
   * Creates a REST-RPC protocol plugin.
   *
   * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
   * @param codec message codec plugin
   * @param errorToException maps a REST-RPC error to a corresponding exception
   * @param exceptionToError maps an exception to a corresponding REST-RPC error
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return REST-RPC protocol plugin
   */
  inline def apply[Node, Codec <: MessageCodec[Node]](
    codec: Codec,
    errorToException: (String, Option[Int]) => Throwable = defaultErrorToException,
    exceptionToError: Throwable => Option[Int] = defaultExceptionToError
  ): RestRpcProtocol[Node, Codec] =
    val encodeRequest = (request: Message.Request[Node]) => codec.encode[Message.Request[Node]](request)
    val decodeRequest = (node: Node) => codec.decode[Message.Request[Node]](node)
    val encodeResponse = (mesponse: Message[Node]) => codec.encode[Message[Node]](mesponse)
    val decodeResponse = (node: Node) => codec.decode[Message[Node]](node)
    val encodeStrings = (value: List[String]) => codec.encode[List[String]](value)
    RestRpcProtocol(
      codec,
      errorToException,
      exceptionToError,
      encodeRequest,
      decodeRequest,
      encodeResponse,
      decodeResponse,
      encodeStrings
    )
