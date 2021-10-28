package automorph.meta

import automorph.codec.json.CirceJsonCodec
import automorph.protocol.JsonRpcProtocol
import automorph.spi.MessageCodec

private[automorph] trait DefaultMeta {

  /** Default message node type. */
  type Node = CirceJsonCodec.Node

  /** Default message codec plugin type. */
  type Codec = CirceJsonCodec

  /**
   * Default RPC protocol plugin type.
   *
   * @tparam PNode message node type
   * @tparam PCodec message codec plugin type
   */
  type Protocol[PNode, PCodec <: MessageCodec[PNode]] = JsonRpcProtocol[PNode, PCodec]

  /**
   * Creates a Circe JSON message codec plugin.
   *
   * @see [[https://www.json.org Message format]]
   * @see [[https://circe.github.io/circe Library documentation]]
   * @see [[https://circe.github.io/circe/api/io/circe/Json.html Node type]]
   * @return message codec plugin
   */
  def codec: Codec = CirceJsonCodec()

  /**
   * Creates a default JSON-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification Protocol specification]]
   * @return RPC protocol plugin
   */
  def protocol: Protocol[Node, Codec] =
    JsonRpcProtocol(codec)

  /**
   * Creates a default JSON-RPC protocol plugin with specified message codec plugin.
   *
   * @see [[https://www.jsonrpc.org/specification Protocol specification]]
   * @param codec message codec plugin
   * @return RPC protocol plugin
   * @tparam PNode message node type
   * @tparam PCodec message codec plugin type
   */
  inline def protocol[PNode, PCodec <: MessageCodec[PNode]](codec: PCodec): Protocol[PNode, PCodec] =
    JsonRpcProtocol(codec)
}