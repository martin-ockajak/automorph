package automorph.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}
import scala.collection.immutable.ArraySeq

/**
 * Byte sequence converter.
 *
 * Converts `ArraySeq.ofByte` to and from specified data type.
 *
 * @tparam T data type
 */
trait Bytes[T] {

  /**
   * Convert specified byte sequence data into a given data type.
   *
   * @param bytes byte sequence
   * @return data
   */
  def to(bytes: ArraySeq.ofByte): T

  /**
   * Convert specified data into a byte sequence.
   *
   * @param data data
   * @return byte sequence
   */
  def from(data: T): ArraySeq.ofByte
}

object Bytes {


  /** `ArraySeq.ofByte` <-> byte sequence converter. */
  implicit val arraySeqOfByte: Bytes[ArraySeq.ofByte] = new Bytes[ArraySeq.ofByte] {

    override def to(bytes: ArraySeq.ofByte): ArraySeq.ofByte = bytes

    override def from(data: ArraySeq.ofByte): ArraySeq.ofByte = data
  }

  /** `Array[Byte]` <-> byte sequence converter. */
  implicit val byteArray: Bytes[Array[Byte]] = new Bytes[Array[Byte]] {

    override def to(bytes: ArraySeq.ofByte): Array[Byte] = bytes.unsafeArray

    override def from(data: Array[Byte]): ArraySeq.ofByte = new ArraySeq.ofByte(data)
  }

  /** `String` <-> byte sequence converter. */
  implicit val string: Bytes[String] = new Bytes[String] {

    /** String character set */
    val charset: Charset = StandardCharsets.UTF_8

    override def to(bytes: ArraySeq.ofByte): String = new String(bytes.unsafeArray, charset)

    override def from(data: String): ArraySeq.ofByte = new ArraySeq.ofByte(data.getBytes(charset))
  }

  /** `ByteBuffer` <-> byte sequence converter. */
  implicit val byteBuffer: Bytes[ByteBuffer] = new Bytes[ByteBuffer] {

    override def to(bytes: ArraySeq.ofByte): ByteBuffer = ByteBuffer.wrap(bytes.unsafeArray)

    override def from(data: ByteBuffer): ArraySeq.ofByte = {
      if (data.hasArray) {
        new ArraySeq.ofByte(data.array)
      } else {
        val array = Array.ofDim[Byte](data.remaining)
        data.get(array, 0, array.length)
        new ArraySeq.ofByte(array)
      }
    }
  }

  /** `InputStream` <-> byte sequence converter. */
  implicit val inputStream: Bytes[InputStream] = new Bytes[InputStream] {

    /** Input stream reading buffer size. */
    val bufferSize = 4096

    override def to(bytes: ArraySeq.ofByte): InputStream = new ByteArrayInputStream(bytes.unsafeArray)

    override def from(data: InputStream): ArraySeq.ofByte = {
      val outputStream = new ByteArrayOutputStream()
      val buffer = Array.ofDim[Byte](bufferSize)
      LazyList.iterate(data.read(buffer)) { length =>
        outputStream.write(buffer, 0, length)
        data.read(buffer)
      }.takeWhile(_ >= 0).take(Int.MaxValue)
      new ArraySeq.ofByte(buffer)
    }
  }
}
