package automorph

import automorph.codec.json.CirceJsonCodec
import automorph.protocol.jsonrpc.JsonRpcProtocol
import automorph.spi.MessageCodec

object DefaultRpcProtocol {

  /** Default RPC protocol plugin type. */
  type Type = JsonRpcProtocol[DefaultMessageCodec.Node, DefaultMessageCodec.Type]

  /**
   * Creates a default RPC protocol plugin.
   *
   * @return RPC protocol plugin
   */
  def apply(): Type = JsonRpcProtocol(DefaultMessageCodec())

  /**
   * Creates a default RPC protocol plugin with specified message ''codec'' plugin.
   *
   * @param codec message codec plugin
   * @return RPC protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   */
  def apply[Node, Codec <: MessageCodec[Node]](codec: Codec): JsonRpcProtocol[Node, Codec] = JsonRpcProtocol(codec)
}
