package automorph

import automorph.protocol.JsonRpcProtocol
import automorph.spi.MessageCodec

private[automorph] trait DefaultMeta {

  /**
   * Default RPC protocol plugin type.
   *
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   */
  type Protocol[Node, Codec <: MessageCodec[Node]] = JsonRpcProtocol[Node, Codec]

  /**
   * Creates a JSON-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification Protocol specification]]
   * @return RPC protocol plugin
   */
  def protocol: Protocol[DefaultMessageCodec.Node, DefaultMessageCodec.Type] =
    JsonRpcProtocol(DefaultMessageCodec(), JsonRpcProtocol.defaultErrorToException, JsonRpcProtocol.defaultExceptionToError, true)

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