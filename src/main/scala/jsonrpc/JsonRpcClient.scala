package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.{EffectContext, JsonContext}
import scala.collection.immutable.ArraySeq

final case class JsonRpcClient[JsonValue, Effect[_]](
  jsonContext: JsonContext[JsonValue],
  effectContext: EffectContext[Effect]):

  def call[Result](method: String, arguments: Seq[Any]): Effect[Result] = ???

  def call[Result](method: String, arguments: Map[String, Any]): Effect[Result] = ???

  def notify(method: String, arguments: Seq[Any]): Effect[Unit] =
    effectContext.unit(())

  def notify(method: String, arguments: Map[String, Any]): Effect[Unit] =
    effectContext.unit(())

  def api[T]: T = ???
