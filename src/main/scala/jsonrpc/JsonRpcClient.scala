package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.{Effect, Codec}
import scala.collection.immutable.ArraySeq

final case class JsonRpcClient[Node, Outcome[_]](
  codec: Codec[Node],
  effect: Effect[Outcome]
):
  def call[Result](method: String, arguments: Seq[Any]): Outcome[Result] = ???

  def call[Result](method: String, arguments: Map[String, Any]): Outcome[Result] = ???

  def notify(method: String, arguments: Seq[Any]): Outcome[Unit] =
    effect.pure(())

  def notify(method: String, arguments: Map[String, Any]): Outcome[Unit] =
    effect.pure(())

  def api[T]: T = ???
