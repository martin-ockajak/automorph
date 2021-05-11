package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import scala.collection.immutable.ArraySeq

final case class CommonJsonRpcServer[Effect[_]](jsonContext: JsonContext, effectContext: EffectContext[Effect]):
  def bind[T <: AnyRef](api: T): Unit =
    ()

  def process(request: ArraySeq[Byte]): Effect[ArraySeq[Byte]] =
    effectContext.unit(request)

  def process(request: InputStream): Effect[InputStream] =
    effectContext.unit(request)

  def process(request: ByteBuffer): Effect[ByteBuffer] =
    effectContext.unit(request)
