package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import jsonrpc.server.ServerMacros
import jsonrpc.spi.{EffectContext, Codec}
import scala.collection.immutable.ArraySeq

final case class JsonRpcServer[Format, Effect[_]](
  jsonContext: Codec[Format],
  effectContext: EffectContext[Effect]):

  private val charset = StandardCharsets.UTF_8.nn
  private val bufferSize = 4096

  inline def bind[T <: AnyRef](api: T): Unit =
    ServerMacros.bind(api)

  def process(request: ArraySeq.ofByte): Effect[ArraySeq.ofByte] =
    val text = new String(request.unsafeArray, charset)
    effectContext.map(
      process(text),
      response =>
        val array:Array[Byte] = response.getBytes(charset).nn
        ArraySeq.ofByte(array)
    )


  def process(request: ByteBuffer): Effect[ByteBuffer] =
    val text = charset.decode(request).toString
    effectContext.map(
      process(text),
      response => charset.encode(response)
    )
    effectContext.pure(request)

  def process(request: InputStream): Effect[InputStream] =
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
    val text = outputStream.toString(charset.name)
    effectContext.map(
      process(text),
      response => new ByteArrayInputStream(response.getBytes(charset).nn)
    )

  private def process(request: String): Effect[String] =
    effectContext.pure(request)
