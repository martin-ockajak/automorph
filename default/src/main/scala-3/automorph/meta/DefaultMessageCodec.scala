package automorph.meta

import automorph.codec.json.CirceJsonCodec
import automorph.protocol.JsonRpcProtocol
import automorph.spi.MessageCodec

private[automorph] trait DefaultMessageCodec {

  /** Default message node type. */
  type Node = CirceJsonCodec.Node

  /** Default message codec plugin type. */
  type Codec = CirceJsonCodec

  /**
   * Default RPC protocol plugin type.
   *
   * @tparam NodeType
   *   message node type
   * @tparam CodecType
   *   message codec plugin type
   * @tparam Context
   *   RPC message context type
   */
  type RpcProtocol[NodeType, CodecType <: MessageCodec[NodeType], Context] =
    JsonRpcProtocol[NodeType, CodecType, Context]

  /**
   * Creates a Circe JSON message codec plugin.
   *
   * @see
   *   [[https://www.json.org Message format]]
   * @see
   *   [[https://circe.github.io/circe Library documentation]]
   * @see
   *   [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
   * @return
   *   message codec plugin
   */
  def messageCodec: Codec =
    CirceJsonCodec()

  /**
   * Creates a default JSON-RPC protocol plugin.
   *
   * @see
   *   [[https://www.jsonrpc.org/specification Protocol specification]]
   * @tparam Context
   *   RPC message context type
   * @return
   *   RPC protocol plugin
   */
  def rpcProtocol[Context]: RpcProtocol[Node, Codec, Context] =
    JsonRpcProtocol(messageCodec)

  /**
   * Creates a default JSON-RPC protocol plugin with specified message codec plugin.
   *
   * @see
   *   [[https://www.jsonrpc.org/specification Protocol specification]]
   * @param messageCodec
   *   message codec plugin
   * @return
   *   RPC protocol plugin
   * @tparam NodeType
   *   message node type
   * @tparam CodecType
   *   message codec plugin type
   * @tparam Context
   *   RPC message context type
   */
  inline def rpcProtocol[NodeType, CodecType <: MessageCodec[NodeType], Context](
    messageCodec: CodecType
  ): RpcProtocol[NodeType, CodecType, Context] =
    JsonRpcProtocol(messageCodec)
}