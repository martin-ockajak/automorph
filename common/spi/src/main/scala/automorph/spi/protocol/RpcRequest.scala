package automorph.spi.protocol

/**
 * RPC request.
 *
 * @constructor Creates RPC request.
 * @param method method name
 * @param arguments method arguments by position or by name
 * @param responseRequired true if the request mandates a response, false if there should be no response
 * @param message RPC message
 * @tparam Node message node type
 * @tparam Details protocol-specific message details type
 */
final case class RpcRequest[Node, Details](
  method: String,
  arguments: Either[List[Node], Map[String, Node]],
  responseRequired: Boolean,
  message: RpcMessage[Details]
)
