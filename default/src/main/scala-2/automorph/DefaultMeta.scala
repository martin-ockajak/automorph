package automorph

import automorph.Default.Codec
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
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   */
  type Protocol[Node, Codec <: MessageCodec[Node]] = JsonRpcProtocol[Node, Codec]

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
   * @return RPC protocol plugin
   */
  def protocol: Protocol[Node, Codec] =
    JsonRpcProtocol(codec, JsonRpcProtocol.defaultErrorToException, JsonRpcProtocol.defaultExceptionToError, true)

  /**
   * Creates a JSON-RPC protocol plugin with specified message codec plugin.
   *
   * @see [[https://www.jsonrpc.org/specification Protocol specification]]
   * @param codec message codec plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return RPC protocol plugin
   */
  def protocol[Node, Codec <: MessageCodec[Node]](codec: Codec): Protocol[Node, Codec] =
    macro applyMacro[Node, Codec]

  def protocolMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]: c.WeakTypeTag](
    c: blackbox.Context
  )(codec: c.Expr[Codec]): c.Expr[Protocol[Node, Codec]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[Any](q"""
      automorph.protocol.JsonRpcProtocol($codec)
    """).asInstanceOf[c.Expr[Protocol[Node, Codec]]]
  }
}
