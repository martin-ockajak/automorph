package automorph.spi.protocol

/**
 * RPC response.
 *
 * @constructor Creates RPC request.
 * @param method method name
 * @param arguments method arguments by position or by name
 * @param respond true if the request mandates a response
 * @param message RPC message
 * @tparam Node message node type
 * @tparam Content protocol-specific message content type
 */
final case class RpcRequest[Node, Content](
  method: String,
  arguments: Either[List[Node], Map[String, Node]],
  respond: Boolean,
  message: RpcMessage[Content]
)
