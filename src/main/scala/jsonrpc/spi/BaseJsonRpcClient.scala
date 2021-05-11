package jsonrpc.spi

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import scala.collection.immutable.ArraySeq

trait BaseJsonRpcClient[Effect[_]]:
  def call[Result](method: String, arguments: Seq[Any]): Effect[Result]

  def call[Result](method: String, arguments: Map[String, Any]): Effect[Result]

  def notify(method: String, arguments: Seq[Any]): Effect[Unit]

  def notify(method: String, arguments: Map[String, Any]): Effect[Unit]

  def api[T]: T
