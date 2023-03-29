package automorph.meta

import automorph.codec.json.CirceJsonCodec
import automorph.protocol.JsonRpcProtocol
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

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
   * Creates a JSON-RPC protocol plugin.
   *
   * @see
   *   [[https://www.jsonrpc.org/specification Protocol specification]]
   * @tparam Context
   *   RPC message context type
   * @return
   *   RPC protocol plugin
   */
  def rpcProtocol[Context]: RpcProtocol[Node, Codec, Context] =
    JsonRpcProtocol(
      messageCodec,
      JsonRpcProtocol.defaultMapError,
      JsonRpcProtocol.defaultMapException,
      true,
      identity,
      identity,
    )

  /**
   * Creates a JSON-RPC protocol plugin with specified message codec plugin.
   *
   * @see
   *   [[https://www.jsonrpc.org/specification Protocol specification]]
   * @param messageCodec
   *   message codec plugin
   * @tparam NodeType
   *   message node type
   * @tparam CodecType
   *   message codec plugin type
   * @tparam Context
   *   RPC message context type
   * @return
   *   RPC protocol plugin
   */
  def rpcProtocol[NodeType, CodecType <: MessageCodec[NodeType], Context](
    messageCodec: CodecType
  ): RpcProtocol[NodeType, CodecType, Context] =
    macro DefaultMessageCodec.rpcProtocolMacro[NodeType, CodecType, Context]
}

object DefaultMessageCodec {

  def rpcProtocolMacro[NodeType, CodecType <: MessageCodec[NodeType], Context](
    c: blackbox.Context
  )(messageCodec: c.Expr[CodecType]): c.Expr[JsonRpcProtocol[NodeType, CodecType, Context]] = {
    import c.universe.Quasiquote

    c.Expr[JsonRpcProtocol[NodeType, CodecType, Context]](q"""
      automorph.protocol.JsonRpcProtocol($messageCodec)
    """)
  }
}
