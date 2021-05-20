package jsonrpc.core

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq
import scala.concurrent.Future
import scala.util.{Success, Try}
import scala.annotation.tailrec

case object EncodingOps:
  private lazy val charset = StandardCharsets.UTF_8.nn

  extension (bytes: Array[Byte]) def asArraySeq: ArraySeq.ofByte = ArraySeq.ofByte(bytes)

  extension (string: String) def toArray: Array[Byte] = string.getBytes(charset).nn

  extension (buffer: ByteBuffer)

    def toArraySeq: ArraySeq.ofByte =
      if buffer.hasArray then
        buffer.array.nn.asArraySeq
      else
        val array = Array.ofDim[Byte](buffer.remaining)
        buffer.get(array, 0, array.size)
        ArraySeq.ofByte(array)

  extension (inputStream: InputStream)

    def toArraySeq(bufferSize: Int): ArraySeq.ofByte =
      val outputStream = ByteArrayOutputStream()
      val buffer = Array.ofDim[Byte](bufferSize)

      @tailrec def copyData(): Unit =
        val length = inputStream.read(buffer)
        if length >= 0 then
          outputStream.write(buffer, 0, length)
          copyData()

      copyData()
      buffer.asArraySeq
