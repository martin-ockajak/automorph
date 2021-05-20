package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.ScalaSupport.asArraySeq
import jsonrpc.server.ServerMacros
import jsonrpc.spi.{Codec, Effect}
import scala.collection.immutable.ArraySeq

final case class JsonRpcServer[Node, Outcome[_]](
  jsonContext: Codec[Node],
  effectContext: Effect[Outcome]):

  private val bufferSize = 4096

  inline def bind[T <: AnyRef](api: T): Unit =
    ServerMacros.bind(api)

  def process(request: ArraySeq.ofByte): Outcome[ArraySeq.ofByte] =
    effectContext.pure(request)

  def process(request: ByteBuffer): Outcome[ByteBuffer] =
    effectContext.map(process(request.asArraySeq), response => {
      ByteBuffer.wrap(response.unsafeArray).nn
    })

  def process(request: InputStream): Outcome[InputStream] =
    effectContext.map(process(request.asArraySeq(bufferSize)), response => {
      ByteArrayInputStream(response.unsafeArray)
    })
