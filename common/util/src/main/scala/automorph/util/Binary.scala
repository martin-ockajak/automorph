package automorph.util

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}
import scala.collection.immutable.ArraySeq
import automorph.util.Binary.Conversion._


case object Binary {

  type Binary = ArraySeq.ofByte

  case object Conversion {

    /** String character set */
    private val charset: Charset = StandardCharsets.UTF_8

    implicit class BinaryImplicit(data: Binary) {
      def toArray: Array[Byte] = data.unsafeArray

      def deserializeToString: String = new String(data.toArray, charset)

      def toByteBuffer: ByteBuffer = ByteBuffer.wrap(data.toArray)

      def toInputStream: InputStream = new ByteArrayInputStream(data.toArray)
    }

    implicit class ByteArrayImplicit(data: Array[Byte]) {
      def toBinary: Binary = new ArraySeq.ofByte(data)

      def toBinary(length: Int): Binary = data.take(length).toBinary
    }

    // string conversion is partially defined (it assumes the binary data is UTF-8 formatted)
    // => nevertheless, it is included for convenience
    implicit class StringImplicit(data: String) {
      def serialize: Binary = data.serializeToArray.toBinary

      def serialize(length: Int): Binary = data.serializeToArray.toBinary(length)

      private[Binary] def serializeToArray: Array[Byte] = data.getBytes(charset)
    }

    implicit class ByteBufferImplicit(data: ByteBuffer) {
      def toBinary: Binary = {
        if (data.hasArray) {
          data.array.toBinary
        } else {
          val array = Array.ofDim[Byte](data.remaining)
          data.get(array)
          new ArraySeq.ofByte(array)
        }
      }

      def toBinary(length: Int): Binary = {
        val array = Array.ofDim[Byte](length)
        data.get(array)
        new ArraySeq.ofByte(array)
      }
    }

    implicit class ImputStreamImplicit(data: InputStream) {
      /** Input stream reading buffer size. */
      private val bufferSize = 4096

      def toBinary: Binary = {
        val outputStream = new ByteArrayOutputStream()
        val buffer = Array.ofDim[Byte](bufferSize)
        // TODO: optimize for performance
        LazyList.iterate(0) { _ =>
          data.read(buffer) match {
            case length if length > 0 =>
              outputStream.write(buffer, 0, length)
              length
            case length => length
          }
        }.takeWhile(_ >= 0).lastOption
        new ArraySeq.ofByte(outputStream.toByteArray)
      }

      def toBinary(length: Int): Binary = {
        val outputStream = new ByteArrayOutputStream(length)
        val buffer = Array.ofDim[Byte](bufferSize)
        // TODO: optimize for performance
        LazyList.iterate(length) { remaining =>
          data.read(buffer, 0, Math.min(remaining, buffer.length)) match {
            case read if read >= 0 =>
              outputStream.write(buffer, 0, read)
              remaining - read
            case _ => 0
          }
        }.takeWhile(_ > 0).lastOption
        new ArraySeq.ofByte(outputStream.toByteArray)
        // TODO: ok if inputStream stays open?
      }
    }
  }


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
     * @param bytes byte sequence
     * @return data
     */
    def to(binary:Binary): T

    /**
     * Convert specified data into a byte sequence.
     *
     * @param data data
     * @return byte sequence
     */
    def from(t: T): Binary

    /**
     * Convert part of specified data into a byte sequence.
     *
     * @param data data
     * @param length converted data length
     * @return byte sequence
     */
    def from(t: T, length: Int): Binary
  }


  case object BinaryConverter{
    // no string converter here, because Binary -> String conversion can fail (assumes UTF-8)

    /** Binary sequence <-> binary sequence converter (identity) */
    implicit val arraySeqOfByte: BinaryConverter[Binary] = new BinaryConverter[Binary] {

      override def to(data: Binary): Binary = data

      override def from(data: Binary): Binary = data

      override def from(bytes: Binary, length: Int): Binary = bytes.toArray.take(length).toBinary
    }

    /** `Array[Byte]` <-> binary sequence converter */
    implicit val byteArray: BinaryConverter[Array[Byte]] = new BinaryConverter[Array[Byte]] {

      override def to(data: Binary): Array[Byte] = data.toArray

      override def from(data: Array[Byte]): Binary = data.toBinary

      override def from(data: Array[Byte], length: Int): Binary = data.toBinary(length)
    }

    /** `ByteBuffer` <-> binary sequence converter */
    implicit val byteBuffer: BinaryConverter[ByteBuffer] = new BinaryConverter[ByteBuffer] {

      override def to(data: Binary): ByteBuffer = data.toByteBuffer

      override def from(data: ByteBuffer): Binary = data.toBinary

      override def from(data: ByteBuffer, length: Int): Binary = data.toBinary(length)
    }

    /** `InputStream` <-> binary sequence converter */
    implicit val inputStream: BinaryConverter[InputStream] = new BinaryConverter[InputStream] {

      override def to(data: Binary): InputStream = data.toInputStream

      override def from(data: InputStream): Binary = data.toBinary

      override def from(data: InputStream, length: Int): Binary = data.toBinary(length)
    }
  }

  // FIXME: remove
  def main(args: Array[String]): Unit = {
    println(
      "abc".serialize.deserializeToString.serialize.toByteBuffer.toBinary.toInputStream.toBinary.deserializeToString
    )
  }
}