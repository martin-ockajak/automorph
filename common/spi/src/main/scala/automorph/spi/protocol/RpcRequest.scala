package automorph.spi.protocol

/**
 * RPC request.
 *
 * @constructor Creates RPC request.
 * @param message RPC message
 * @param function invoked function name
 * @param arguments invoked function arguments by name or by position
 * @param responseRequired true if this request mandates a response, false if there should be no response
 * @param id request correlation identifier
 * @tparam Node message node type
 * @tparam Metadata protocol-specific message metadata type
 */
final case class RpcRequest[Node, Metadata](
  message: RpcMessage[Metadata],
  function: String,
  arguments: Seq[Either[Node, (String, Node)]],
  responseRequired: Boolean,
  id: String
)
