package jsonrpc.core

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import scala.collection.immutable.ArraySeq
import scala.concurrent.Future
import scala.util.{Success, Try}

case object EncodingOps:
  private lazy val charset = StandardCharsets.UTF_8.nn

  extension (bytes: Array[Byte])
    def decodeToString: String = String(bytes, charset)

    def asArraySeq: ArraySeq.ofByte = ArraySeq.ofByte(bytes)

  extension (string: String)
    def encodeToBytes: Array[Byte] = string.getBytes(charset).nn

    def encodeToByteBuffer: ByteBuffer = charset.encode(string).nn

  extension (buffer: ByteBuffer)
    def asArraySeq: ArraySeq.ofByte =
      if buffer.hasArray then
        ArraySeq.ofByte(buffer.array.nn)
      else
        val array = Array.ofDim[Byte](buffer.remaining)
        buffer.get(array, 0, array.size)
        ArraySeq.ofByte(array)

    def decodeToString: String = charset.decode(buffer).toString

  extension (outputStream: ByteArrayOutputStream) def decodeToString: String = outputStream.toString(charset.name)

  extension (inputStream: InputStream)

    def asArraySeq(bufferSize: Int): ArraySeq.ofByte =
      val outputStream = ByteArrayOutputStream()
      val buffer = Array.ofDim[Byte](bufferSize)

      while
        val length = inputStream.read(buffer)
        if length >= 0 then
          outputStream.write(buffer, 0, length)
          true
        else
          false
      do ()
      buffer.asArraySeq
