package automorph.spi.protocol

/**
 * RPC request.
 *
 * @constructor Creates RPC request.
 * @param function invoked function name
 * @param arguments invoked function arguments by name or by position
 * @param responseRequired true if this request mandates a response, false if there should be no response
 * @param message RPC message
 * @tparam Node message node type
 * @tparam Details protocol-specific message details type
 */
final case class RpcRequest[Node, Details](
  function: String,
  arguments: Either[List[Node], Map[String, Node]],
  responseRequired: Boolean,
  message: RpcMessage[Details]
)
