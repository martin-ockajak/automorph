package automorph.protocol

import automorph.protocol.restrpc.{ErrorMapping, Message, RestRpcCore}
import automorph.spi.{MessageCodec, RpcProtocol}
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * REST-RPC protocol plugin.
 *
 * @constructor Creates a REST-RPC protocol plugin.
 * @see [[https://automorph.org/rest-rpc REST-RPC protocol specification]]
 * @param codec message codec plugin
 * @param mapError maps a REST-RPC error to a corresponding exception
 * @param mapException maps an exception to a corresponding REST-RPC error
 * @param encodeRequest converts a REST-RPC request to message format node
 * @param decodeRequest converts a message format node to REST-RPC request
 * @param encodeResponse converts a REST-RPC response to message format node
 * @param decodeResponse converts a message format node to REST-RPC response
 * @param encodeStrings converts list of strings to message format node
 * @tparam Node message node type
 * @tparam Codec message codec plugin type
 */
final case class RestRpcProtocol[Node, Codec <: MessageCodec[Node]](
  codec: Codec,
  mapError: (String, Option[Int]) => Throwable = RestRpcProtocol.defaultMapError,
  mapException: Throwable => Option[Int] = RestRpcProtocol.defaultMapException,
  protected val encodeRequest: Message.Request[Node] => Node,
  protected val decodeRequest: Node => Message.Request[Node],
  protected val encodeResponse: Message[Node] => Node,
  protected val decodeResponse: Node => Message[Node],
  protected val encodeStrings: List[String] => Node
) extends RestRpcCore[Node, Codec] with RpcProtocol[Node, Codec]

object RestRpcProtocol extends ErrorMapping {

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
    errorToException: (String, Option[Int]) => Throwable,
    exceptionToError: Throwable => Option[Int]
  ): RestRpcProtocol[Node, Codec] =
    macro applyMacro[Node, Codec]

  /**
   * Creates a REST-RPC protocol plugin.
   *
   * @see [[https://www.jsonrpc.org/specification REST-RPC protocol specification]]
   * @param codec message codec plugin
   * @tparam Node message node type
   * @tparam Codec message codec plugin type
   * @return REST-RPC protocol plugin
   */
  def apply[Node, Codec <: MessageCodec[Node]](codec: Codec): RestRpcProtocol[Node, Codec] =
    macro applyDefaultsMacro[Node, Codec]

  def applyMacro[Node: c.WeakTypeTag, Codec <: MessageCodec[Node]](c: blackbox.Context)(
    codec: c.Expr[Codec],
    errorToException: c.Expr[(String, Option[Int]) => Throwable],
    exceptionToError: c.Expr[Throwable => Option[Int]]
  ): c.Expr[RestRpcProtocol[Node, Codec]] = {
    import c.universe.{Quasiquote, weakTypeOf}
    Seq(weakTypeOf[Node], weakTypeOf[Codec])

    c.Expr[RestRpcProtocol[Node, Codec]](q"""
      new automorph.protocol.RestRpcProtocol(
        $codec,
        $errorToException,
        $exceptionToError,
        request => $codec.encode[automorph.protocol.restrpc.Message.Request[${weakTypeOf[Node]}]](request),
        node => $codec.decode[automorph.protocol.restrpc.Message.Request[${weakTypeOf[Node]}]](node),
        response => $codec.encode[automorph.protocol.restrpc.Message[${weakTypeOf[Node]}]](response),
        node => $codec.decode[automorph.protocol.restrpc.Message[${weakTypeOf[Node]}]](node),
        value => $codec.encode[List[String]](value)
      )
    """)
  }

  def applyDefaultsMacro[Node, Codec <: MessageCodec[Node]](c: blackbox.Context)(
    codec: c.Expr[Codec]
  ): c.Expr[RestRpcProtocol[Node, Codec]] = {
    import c.universe.Quasiquote

    c.Expr[RestRpcProtocol[Node, Codec]](q"""
      automorph.protocol.RestRpcProtocol(
        $codec,
        automorph.protocol.RestRpcProtocol.defaultErrorToException,
        automorph.protocol.RestRpcProtocol.defaultExceptionToError
      )
    """)
  }
}
