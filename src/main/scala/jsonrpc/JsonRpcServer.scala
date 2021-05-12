package jsonrpc

import java.io.{InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.server.ServerMacros
import jsonrpc.spi.{EffectContext, JsonContext}
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

final case class JsonRpcServer[Effect[_]](
  jsonContext: JsonContext,
  effectContext: EffectContext[Effect]):

  inline def bind[T <: AnyRef](api: T): Unit = {
    ServerMacros.bind(api)
  }

  def process(request: ArraySeq[Byte]): Effect[ArraySeq[Byte]] =
    effectContext.unit(request)

  def process(request: InputStream): Effect[InputStream] =
    effectContext.unit(request)

  def process(request: ByteBuffer): Effect[ByteBuffer] =
    effectContext.unit(request)
