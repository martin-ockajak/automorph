package jsonrpc

import jsonrpc.core.Protocol
import jsonrpc.spi.{CallError, Codec, Effect}
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC client.
 *
 * @param codec hierarchical data format codec plugin
 * @param effect computation effect system plugin
 * @param transport message transport
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 */
final case class JsonRpcClient[Node, Outcome[_]](
  codec: Codec[Node],
  effect: Effect[Outcome],
  transport: JsonRpcTransport[Outcome]
):

  def call[R](method: String, arguments: Seq[Any]): Outcome[R] = call(method, encodeArguments(arguments))

  def call[R](method: String, arguments: Map[String, Any]): Outcome[R] = call(method, encodeArguments(arguments))

  def notify(method: String, arguments: Seq[Any]): Outcome[Unit] = notify(method, encodeArguments(arguments))

  def notify(method: String, arguments: Map[String, Any]): Outcome[Unit] = notify(method, encodeArguments(arguments))

  def proxy[T]: T = ???

  private def encodeArguments(arguments: Seq[Any]): Node = ???

  private def encodeArguments(arguments: Map[String, Any]): Node = ???

  private def decodeResult[R](value: Either[CallError[Node], Node]): Either[Throwable, R] = ???

  private def requestMessage(id: Boolean, method: String, arguments: Node): ArraySeq.ofByte = ???

  private def responseMessage(response: ArraySeq.ofByte): Protocol.Response[Node] = ???

  private def call[R](method: String, arguments: Node): Outcome[R] =
    val request = requestMessage(id = true, method, arguments)
    effect.map(
      transport.call(request),
      response =>
        val message = responseMessage(response)
        decodeResult(message.value) match
          case Left(error)   => throw error
          case Right(result) => result
    )

  private def notify[R](method: String, arguments: Node): Outcome[Unit] =
    val request = requestMessage(id = false, method, arguments)
    effect.map(transport.call(request), _ => ())
