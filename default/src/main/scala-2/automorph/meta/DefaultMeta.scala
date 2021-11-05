package automorph.meta

import automorph.codec.json.CirceJsonCodec
import automorph.protocol.JsonRpcProtocol
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

private[automorph] trait DefaultMeta {

  /** Default message node type. */
  type Node = CirceJsonCodec.Node

  /** Default message codec plugin type. */
  type Codec = CirceJsonCodec

  /**
   * Default RPC protocol plugin type.
   *
   * @tparam NodeType message node type
   * @tparam CodecType message codec plugin type
   * @tparam Context message context type
   */
  type Protocol[NodeType, CodecType <: MessageCodec[NodeType], Context] = JsonRpcProtocol[NodeType, CodecType, Context]

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
   * Creates a JSON-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification Protocol specification]]
   * @tparam Context message context type
   * @return RPC protocol plugin
   */
  def protocol[Context]: Protocol[Node, Codec, Context] =
    JsonRpcProtocol(codec, JsonRpcProtocol.defaultMapError, JsonRpcProtocol.defaultMapException, true)

  /**
   * Creates a JSON-RPC protocol plugin with specified message codec plugin.
   *
   * @see [[https://www.jsonrpc.org/specification Protocol specification]]
   * @param codec message codec plugin
   * @tparam NodeType message node type
   * @tparam CodecType message codec plugin type
   * @tparam Context message context type
   * @return RPC protocol plugin
   */
  def protocol[NodeType, CodecType <: MessageCodec[NodeType], Context](
    codec: CodecType
  ): Protocol[NodeType, CodecType, Context] =
    macro DefaultMeta.protocolMacro[NodeType, CodecType, Context]
}

object DefaultMeta {

  def protocolMacro[NodeType, CodecType <: MessageCodec[NodeType], Context](
    c: blackbox.Context
  )(codec: c.Expr[CodecType]): c.Expr[JsonRpcProtocol[NodeType, CodecType, Context]] = {
    import c.universe.Quasiquote

    c.Expr[JsonRpcProtocol[NodeType, CodecType, Context]](q"""
      automorph.protocol.JsonRpcProtocol($codec)
    """)
  }
}
