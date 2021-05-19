package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.{EffectContext, FormatContext}
import scala.collection.immutable.ArraySeq

final case class JsonRpcClient[Format, Effect[_]](
  jsonContext: FormatContext[Format],
  effectContext: EffectContext[Effect]):

  def call[Result](method: String, arguments: Seq[Any]): Effect[Result] = ???

  def call[Result](method: String, arguments: Map[String, Any]): Effect[Result] = ???

  def notify(method: String, arguments: Seq[Any]): Effect[Unit] =
    effectContext.pure(())

  def notify(method: String, arguments: Map[String, Any]): Effect[Unit] =
    effectContext.pure(())

  def api[T]: T = ???
