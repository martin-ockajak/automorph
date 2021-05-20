package jsonrpc.core

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq
import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.annotation.tailrec

case object EncodingOps:

  private lazy val charset = StandardCharsets.UTF_8
  private val maxReadIterations = 1000_000

  extension (bytes: Array[Byte]) def asArraySeq: ArraySeq.ofByte = ArraySeq.ofByte(bytes)

  extension (string: String) def toArraySeq: ArraySeq.ofByte = string.getBytes(charset).asArraySeq

  extension (buffer: ByteBuffer)

    def toArraySeq: ArraySeq.ofByte =
      if buffer.hasArray then
        buffer.array.asArraySeq
      else
        val array = Array.ofDim[Byte](buffer.remaining)
        buffer.get(array, 0, array.size)
        ArraySeq.ofByte(array)

  extension (inputStream: InputStream)

    def toArraySeq(bufferSize: Int): ArraySeq.ofByte =
      val outputStream = ByteArrayOutputStream()
      val buffer = Array.ofDim[Byte](bufferSize)
      LazyList.iterate(inputStream.read(buffer))(length =>
        outputStream.write(buffer, 0, length)
        inputStream.read(buffer)
      ).takeWhile(_ >= 0).take(maxReadIterations)
      buffer.asArraySeq
