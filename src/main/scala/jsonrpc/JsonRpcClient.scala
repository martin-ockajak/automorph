package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.{Effect, Codec}
import scala.collection.immutable.ArraySeq

/**
 * JSON-RPC client.
 *
 * @param codec hierarchical data format codec plugin
 * @param effect computation effect system plugin
 * @tparam Node data format node representation type
 * @tparam Outcome computation outcome effect type
 */
final case class JsonRpcClient[Node, Outcome[_]](
  codec: Codec[Node],
  effect: Effect[Outcome]
):
  def call[R](method: String, arguments: Seq[Any]): Outcome[R] = ???

  def call[R](method: String, arguments: Map[String, Any]): Outcome[R] = ???

  def notify(method: String, arguments: Seq[Any]): Outcome[Unit] = ???

  def notify(method: String, arguments: Map[String, Any]): Outcome[Unit] = ???

  def api[T]: T = ???
