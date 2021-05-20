package jsonrpc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.core.ScalaSupport.*
import jsonrpc.server.ServerMacros
import jsonrpc.spi.{Effect, Codec}
import scala.collection.immutable.ArraySeq

final case class JsonRpcServer[DOM, E[_]](
  jsonContext: Codec[DOM],
  effectContext: Effect[E]):

  private val bufferSize = 4096

  inline def bind[T <: AnyRef](api: T): Unit =
    ServerMacros.bind(api)

  def process(request: ArraySeq.ofByte): E[ArraySeq.ofByte] =
    val text = request.unsafeArray.decodeToString
    effectContext.map(
      process(text),
      response => response.encodeToBytes.asArraySeq
    )

  def process(request: ByteBuffer): E[ByteBuffer] =
    val text = request.decodeToString
    effectContext.map(
      process(text),
      response => response.encodeToByteBuffer
    )
    effectContext.pure(request)

  def process(request: InputStream): E[InputStream] =
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

  private def process(request: String): E[String] =
    effectContext.pure(request)
