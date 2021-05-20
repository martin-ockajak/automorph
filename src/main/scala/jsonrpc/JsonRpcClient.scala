package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.{Effect, Codec}
import scala.collection.immutable.ArraySeq

final case class JsonRpcClient[Node, Outcome[_]](
  jsonContext: Codec[Node],
  effectContext: Effect[Outcome]):

  def call[Result](method: String, arguments: Seq[Any]): Outcome[Result] = ???

  def call[Result](method: String, arguments: Map[String, Any]): Outcome[Result] = ???

  def notify(method: String, arguments: Seq[Any]): Outcome[Unit] =
    effectContext.pure(())

  def notify(method: String, arguments: Map[String, Any]): Outcome[Unit] =
    effectContext.pure(())

  def api[T]: T = ???
