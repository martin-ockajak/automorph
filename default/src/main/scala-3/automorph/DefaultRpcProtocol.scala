package automorph

import automorph.codec.json.CirceJsonRpc
import automorph.protocol.JsonRpcProtocol
import automorph.spi.MessageCodec
import io.circe.{Decoder, Encoder}

object DefaultRpcProtocol:

  /**
   * Default RPC protocol plugin type.
   *
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   */
  type Type[Node, Codec <: MessageCodec[Node]] = JsonRpcProtocol[Node, Codec]

  /**
   * Creates a default RPC protocol plugin.
   *
   * @return RPC protocol plugin
   */
  def apply(): Type[DefaultMessageCodec.Node, DefaultMessageCodec.Type] =
    // FIXME - remove
    given Encoder[CirceJsonRpc.RpcMessage] = CirceJsonRpc.messageEncoder
    given Decoder[CirceJsonRpc.RpcMessage] = CirceJsonRpc.messageDecoder

    JsonRpcProtocol(DefaultMessageCodec())

  /**
   * Creates a default RPC protocol plugin with specified message ''codec'' plugin.
   *
   * @param codec message codec plugin
   * @return RPC protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   */
  inline def apply[Node, Codec <: MessageCodec[Node]](codec: Codec): Type[Node, Codec] =
    JsonRpcProtocol(codec)
