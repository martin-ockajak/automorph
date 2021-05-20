package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.ScalaSupport.{asArraySeq, decodeToString, encodeToBytes, encodeToByteBuffer}
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
    val text = request.unsafeArray.decodeToString
    effectContext.map(process(text), _.encodeToBytes.asArraySeq)

  def process(request: ByteBuffer): Outcome[ByteBuffer] =
    val text = request.decodeToString
    effectContext.map(process(text), _.encodeToByteBuffer)
    effectContext.pure(request)

  def process(request: InputStream): Outcome[InputStream] =
    val outputStream = new ByteArrayOutputStream
    val buffer = Array.ofDim[Byte](bufferSize)

    while
      val length = request.read(buffer)
      if length >= 0 then
        outputStream.write(buffer, 0, length)
        true
      else
        false
    do ()

    val text = outputStream.decodeToString
    effectContext.map(
      process(text),
      response => new ByteArrayInputStream(response.encodeToBytes)
    )

  private def process(request: String): Outcome[String] =
    effectContext.pure(request)
