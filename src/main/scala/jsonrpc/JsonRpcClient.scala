package jsonrpc

import jsonrpc.core.Protocol
import jsonrpc.spi.{CallError, Codec, Effect}
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC client.
 *
 * @see [[https://www.jsonrpc.org/specification JSON-RPC protocol specification]]
 * @constructor Create a new JSON-RPC client using the specified `codec` and `effect` implementations.
 * @param codec hierarchical data format codec plugin
 * @param effect computation effect system plugin
 * @param transport message transport layer
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 * @tparam Context request context type
 */
final case class JsonRpcClient[Node, Outcome[_], Context](
  codec: Codec[Node],
  effect: Effect[Outcome],
  transport: JsonRpcTransport[Outcome, Context]
):

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the arguments ''by position''.
   *
   * @param method method name
   * @param arguments arguments by position
   * @tparam R result type
   * @return result value
   */
  def call[R](method: String, arguments: Seq[Any]): Outcome[R] = call(method, arguments)(using None)

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the ''arguments by name''.
   *
   * @param method method name
   * @param arguments arguments by position
   * @tparam R result type
   * @return result value
   */
  def call[R](method: String, arguments: Map[String, Any]): Outcome[R] = call(method, arguments)(using None)

  /**
   * Perform a remote JSON-RPC method call'' supplying the ''arguments by position''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by position
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return result value
   */
  def call[R](method: String, arguments: Seq[Any])(using context: Option[Context]): Outcome[R] =
    rpcCall(method, encodeArguments(arguments), context)

  /**
   * Perform a remote JSON-RPC method ''call'' supplying the ''arguments by name''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by position
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return result value
   */
  def call[R](method: String, arguments: Map[String, Any])(using context: Option[Context]): Outcome[R] =
    rpcCall(method, encodeArguments(arguments), context)

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by position''.
   *
   * @param method method name
   * @param arguments arguments by position
   * @tparam R result type
   * @return nothing
   */
  def notify(method: String, arguments: Seq[Any]): Outcome[Unit] = notify(method, arguments)(using None)

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by name''.
   *
   * @param method method name
   * @param arguments arguments by position
   * @tparam R result type
   * @return nothing
   */
  def notify(method: String, arguments: Map[String, Any]): Outcome[Unit] = notify(method, arguments)(using None)

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by position''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by position
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return nothing
   */
  def notify(method: String, arguments: Seq[Any])(using context: Option[Context]): Outcome[Unit] =
    rpcNotify(method, encodeArguments(arguments), context)

  /**
   * Perform a remote JSON-RPC method ''notification'' supplying the ''arguments by name''.
   *
   * The specified ''context'' may be used to supply additional information needed to send the request.
   *
   * @param method method name
   * @param arguments arguments by position
   * @param context JSON-RPC request context
   * @tparam R result type
   * @return nothing
   */
  def notify(method: String, arguments: Map[String, Any])(using context: Option[Context]): Outcome[Unit] =
    rpcNotify(method, encodeArguments(arguments), context)

  /**
   * Create a ''transparent proxy instance'' of a remote JSON-RPC API.
   * Invocations of local proxy methods are translated into remote JSON-API calls.
   *
   * @tparam T remote API type
   * @return remote API proxy instance
   */
  def proxy[T]: T = ???

  private def encodeArguments(arguments: Seq[Any]): Node = ???

  private def encodeArguments(arguments: Map[String, Any]): Node = ???

  private def decodeResult[R](value: Either[CallError[Node], Node]): Either[Throwable, R] = ???

  private def requestMessage(id: Boolean, method: String, arguments: Node): ArraySeq.ofByte = ???

  private def responseMessage(response: ArraySeq.ofByte): Protocol.Response[Node] = ???

  private def rpcCall[R](method: String, arguments: Node, context: Option[Context]): Outcome[R] =
    val request = requestMessage(id = true, method, arguments)
    effect.map(
      transport.call(request, context),
      response =>
        val message = responseMessage(response)
        decodeResult(message.value) match
          case Left(error)   => throw error
          case Right(result) => result
    )

  private def rpcNotify[R](method: String, arguments: Node, context: Option[Context]): Outcome[Unit] =
    val request = requestMessage(id = false, method, arguments)
    effect.map(transport.call(request, context), _ => ())
