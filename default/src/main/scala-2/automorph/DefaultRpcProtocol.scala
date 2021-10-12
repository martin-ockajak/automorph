package automorph

import automorph.protocol.JsonRpcProtocol
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
   * Creates a JSON-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification Protocol specification]]
   * @return RPC protocol plugin
   */
  def apply(): Type[DefaultMessageCodec.Node, DefaultMessageCodec.Type] =
    JsonRpcProtocol(DefaultMessageCodec(), JsonRpcProtocol.defaultErrorToException, JsonRpcProtocol.defaultExceptionToError)

  /**
   * Creates a JSON-RPC protocol plugin with specified message codec plugin.
   *
   * @see [[https://www.jsonrpc.org/specification Protocol specification]]
   * @param codec message codec plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node]](codec: Codec): Type[Node, Codec] =
    macro applyMacro[Node, Codec]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]: c.WeakTypeTag](
    c: blackbox.Context
  )(codec: c.Expr[Codec]): c.Expr[Type[Node, Codec]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[Any](q"""
      new automorph.protocol.JsonRpcProtocol($codec)
    """).asInstanceOf[c.Expr[Type[Node, Codec]]]
  }
}
