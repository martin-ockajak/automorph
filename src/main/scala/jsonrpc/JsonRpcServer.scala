package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.server.ServerMacros
import jsonrpc.spi.{EffectContext, JsonContext}
import scala.collection.immutable.ArraySeq

final case class JsonRpcServer[Effect[_]](jsonContext: JsonContext, effectContext: EffectContext[Effect]):
  def bind[T <: AnyRef](api: T): Unit = {
    ServerMacros.print(api.toString)
    ()
  }

  def process(request: ArraySeq[Byte]): Effect[ArraySeq[Byte]] =
    effectContext.unit(request)

  def process(request: InputStream): Effect[InputStream] =
    effectContext.unit(request)

  def process(request: ByteBuffer): Effect[ByteBuffer] =
    effectContext.unit(request)
