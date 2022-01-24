package automorph.util

import automorph.util.BinaryConverter.Binary
import automorph.util.Extensions.{BinaryOps, ByteArrayOps, ByteBufferOps, InputStreamOps, StringOps}
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
trait BinaryConverter[T] {

  /**
   * Convert specified byte sequence data into a given data type.
   *
   * @param binary byte sequence
   * @return data
   */
  def to(binary: Binary): T

  /**
   * Convert specified data into a byte sequence.
   *
   * @param data data
   * @return byte sequence
   */
  def from(data: T): Binary

  /**
   * Convert part of specified data into a byte sequence.
   *
   * @param data   data
   * @param length converted data length
   * @return byte sequence
   */
  def from(data: T, length: Int): Binary
}

object BinaryConverter {

  /** Immutable byte array. */
  type Binary = ArraySeq.ofByte

  /** Binary sequence <-> binary sequence converter (identity) */
  implicit val binary: BinaryConverter[Binary] = new BinaryConverter[Binary] {

    override def to(data: Binary): Binary = data

    override def from(data: Binary): Binary = data

    override def from(bytes: Binary, length: Int): Binary =
      bytes.toArray.take(length).toBinary
  }

  /** `Array[Byte]` <-> binary sequence converter */
  implicit val byteArray: BinaryConverter[Array[Byte]] = new BinaryConverter[Array[Byte]] {

    override def to(data: Binary): Array[Byte] = data.toArray

    override def from(data: Array[Byte]): Binary = data.toBinary

    override def from(data: Array[Byte], length: Int): Binary =
      data.toBinary(length)
  }

  /** `ByteBuffer` <-> binary sequence converter */
  implicit val byteBuffer: BinaryConverter[ByteBuffer] = new BinaryConverter[ByteBuffer] {

    override def to(data: Binary): ByteBuffer = data.toByteBuffer

    override def from(data: ByteBuffer): Binary = data.toBinary

    override def from(data: ByteBuffer, length: Int): Binary =
      data.toBinary(length)
  }

  /** `InputStream` <-> binary sequence converter */
  implicit val inputStream: BinaryConverter[InputStream] = new BinaryConverter[InputStream] {

    override def to(data: Binary): InputStream = data.toInputStream

    override def from(data: InputStream): Binary = data.toBinary

    override def from(data: InputStream, length: Int): Binary =
      data.toBinary(length)
  }
}
