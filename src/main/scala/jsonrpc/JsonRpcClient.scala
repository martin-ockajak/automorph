package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.{Effect, Codec}
import scala.collection.immutable.ArraySeq

final case class JsonRpcClient[DOM, E[_]](
  jsonContext: Codec[DOM],
  effectContext: Effect[E]):

  def call[Result](method: String, arguments: Seq[Any]): E[Result] = ???

  def call[Result](method: String, arguments: Map[String, Any]): E[Result] = ???

  def notify(method: String, arguments: Seq[Any]): E[Unit] =
    effectContext.pure(())

  def notify(method: String, arguments: Map[String, Any]): E[Unit] =
    effectContext.pure(())

  def api[T]: T = ???
