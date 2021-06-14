package jsonrpc.util

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq

case object Encoding {

  private val charset = StandardCharsets.UTF_8
  private val maxReadIterations = 1024 * 1024

  def toArraySeq(string: String): ArraySeq.ofByte = new ArraySeq.ofByte(string.getBytes(charset))

  def toArraySeq(buffer: ByteBuffer): ArraySeq.ofByte =
    if (buffer.hasArray) {
      new ArraySeq.ofByte(buffer.array)
    } else {
      val array = Array.ofDim[Byte](buffer.remaining)
      buffer.get(array, 0, array.size)
      new ArraySeq.ofByte(array)
    }

  def toArraySeq(inputStream: InputStream, bufferSize: Int): ArraySeq.ofByte = {
    val outputStream = new ByteArrayOutputStream()
    val buffer = Array.ofDim[Byte](bufferSize)
    LazyList.iterate(inputStream.read(buffer)) { length =>
      outputStream.write(buffer, 0, length)
      inputStream.read(buffer)
    }.takeWhile(_ >= 0).take(maxReadIterations)
    new ArraySeq.ofByte(buffer)
  }
}
