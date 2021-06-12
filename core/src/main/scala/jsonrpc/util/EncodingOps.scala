package jsonrpc.util

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.annotation.tailrec
import scala.collection.immutable.ArraySeq
import scala.concurrent.Future
import scala.util.{Success, Try}

case object EncodingOps {

  private val charset = StandardCharsets.UTF_8
  private val maxReadIterations = 1024 * 1024

  extension (string: String) def toArraySeq: ArraySeq.ofByte = ArraySeq.ofByte(string.getBytes(charset))

  extension (buffer: ByteBuffer)

    def toArraySeq: ArraySeq.ofByte = {
      if (buffer.hasArray) {
        ArraySeq.ofByte(buffer.array)
      } else {
        val array = Array.ofDim[Byte](buffer.remaining)
        buffer.get(array, 0, array.size)
        ArraySeq.ofByte(array)
      }
    }

  extension (inputStream: InputStream)

    def toArraySeq(bufferSize: Int): ArraySeq.ofByte = {
      val outputStream = ByteArrayOutputStream()
      val buffer = Array.ofDim[Byte](bufferSize)
      LazyList.iterate(inputStream.read(buffer))(length =>
        outputStream.write(buffer, 0, length)
          inputStream.read(buffer)
      ).takeWhile(_ >= 0).take(maxReadIterations)
      ArraySeq.ofByte(buffer)
    }
}
