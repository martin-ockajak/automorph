package automorph.protocol

import automorph.protocol.restrpc.{ErrorMapping, RestRpcCore}
import automorph.spi.{MessageCodec, RpcProtocol}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * REST-RPC protocol plugin.
 *
 * @constructor Creates a REST-RPC protocol plugin.
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @param codec message codec plugin
 * @param errorToException maps a REST-RPC error to a corresponding exception
 * @param exceptionToError maps an exception to a corresponding REST-RPC error
 * @param encodeStrings converts list of strings to message codec node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class RestRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  errorToException: (String, Option[Int]) => Throwable,
  exceptionToError: Throwable => Option[Int],
  protected val encodeStrings: List[String] => Node
) extends RestRpcCore[Node, Codec] with RpcProtocol[Node]

case object RestRpcProtocol extends ErrorMapping {

  /**
   * Creates a REST-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification REST-RPC protocol specification]]
   * @param codec message codec plugin
   * @param errorToException maps a REST-RPC error to a corresponding exception
   * @param exceptionToError maps an exception to a corresponding REST-RPC error
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return REST-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node]](
    codec: Codec,
    errorToException: (String, Option[Int]) => Throwable = defaultErrorToException,
    exceptionToError: Throwable => Option[Int] = defaultExceptionToError
  ): RestRpcProtocol[Node, Codec] =
    macro applyMacro[Node, Codec]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]: c.WeakTypeTag](c: blackbox.Context)(
    codec: c.Expr[Codec],
    errorToException: c.Expr[(String, Option[Int]) => Throwable],
    exceptionToError: c.Expr[Throwable => Option[Int]]
  ): c.Expr[RestRpcProtocol[Node, Codec]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[Any](q"""
      new automorph.protocol.RestRpcProtocol(
        $codec,
        $errorToException,
        $exceptionToError,
        value => $codec.encode[List[String]](value)
      )
    """).asInstanceOf[c.Expr[RestRpcProtocol[Node, Codec]]]
  }
}
