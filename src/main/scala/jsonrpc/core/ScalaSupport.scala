package jsonrpc.core

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import scala.collection.immutable.ArraySeq
import scala.concurrent.Future
import scala.util.{Try, Success}

case object ScalaSupport:
  private lazy val charset = StandardCharsets.UTF_8.nn

  extension [T](value:T)
    def some: Option[T] = Some(value)

    def asCompletedFuture: Future[T] = Future.successful(value)

    def asSuccess: Try[T] = Success(value)

    def asLeft[R] :Either[T, R] = Left(value)

    def asRight[L] :Either[L, T] = Right(value)

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

  extension (outputStream: ByteArrayOutputStream)
    def decodeToString: String = outputStream.toString(charset.name)
