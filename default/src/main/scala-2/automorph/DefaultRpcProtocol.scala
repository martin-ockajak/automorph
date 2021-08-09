package automorph

import automorph.protocol.jsonrpc.JsonRpcProtocol
import automorph.spi.MessageCodec
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object DefaultRpcProtocol {

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
    macro apply0Macro

  /**
   * Creates a default RPC protocol plugin with specified message ''codec'' plugin.
   *
   * @param codec message codec plugin
   * @return RPC protocol plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   */
  def apply[Node, Codec <: MessageCodec[Node]](codec: Codec): Type[Node, Codec] =
    macro apply1Macro

  def apply0Macro(c: blackbox.Context): c.Expr[Type] = {
    import c.universe.Quasiquote

    c.Expr[Any](q"""
      new automorph.protocol.JsonRpcProtocol(automorph.DefaultMessageCodec())
    """).asInstanceOf[c.Expr[Type[Node, Codec]]
  }

  def apply1Macro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]: c.WeakTypeTag](
    c: blackbox.Context
  )(codec: c.Expr[Codec]): c.Expr[Type[Node, Codec]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[Any](q"""
      new automorph.protocol.JsonRpcProtocol($codec)
    """).asInstanceOf[c.Expr[Type[Node, Codec]]
  }
}
