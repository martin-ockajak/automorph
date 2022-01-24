package automorph.util

import automorph.spi.EffectSystem
import automorph.util.BinaryConverter.Binary

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.{Charset, StandardCharsets}
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}

private[automorph] object Extensions {

  /** String character set */
  private val charset: Charset = StandardCharsets.UTF_8

  implicit final class ThrowableOps(private val throwable: Throwable) {

    /**
     * Assemble detailed trace of an exception and its causes.
     *
     * @return error messages
     */
    def trace: Seq[String] = trace()

    /**
     * Assemble detailed trace of an exception and its causes.
     *
     * @param maxCauses maximum number of included exception causes
     * @return error messages
     */
    def trace(maxCauses: Int = 100): Seq[String] =
      LazyList.iterate(Option(throwable))(_.flatMap(error => Option(error.getCause)))
        .takeWhile(_.isDefined).flatten.take(maxCauses).map { throwable =>
          val exceptionName = throwable.getClass.getSimpleName
          val message = Option(throwable.getMessage).getOrElse("")
          s"[$exceptionName] $message"
        }

    /**
     * Assemble detailed description of an exception and its causes.
     *
     * @return error messages
     */
     def description: String = trace.mkString("\n")
  }

  implicit final class TryOps[T](private val tryValue: Try[T]) {

    /**
     * Creates a new 'Try' by applying ''onFailure'' on `Failure` or returns this on `Success`.
     *
     * @param onFailure function to apply if this is a `Failure`
     * @return a transformed `Try`
     */
    def onFailure(onFailure: Throwable => Unit): Try[T] = tryValue.recoverWith { case error =>
      onFailure(error)
      Failure(error)
    }

    /**
     * Applies ''onException'' on `Failure` or ''onSuccess'' on `Success`.
     *
     * @param onFailure function to apply if this is a `Success`
     * @param onSuccess function to apply if this is a `Failure`
     * @tparam U result type
     * @return applied function result
     */
    def pureFold[U](onFailure: Throwable => U, onSuccess: T => U): U = tryValue match {
      case Failure(error) => onFailure(error)
      case Success(value) => onSuccess(value)
    }
  }

  implicit final class EffectOps[Effect[_], T](private val effect: Effect[T]) {

    /**
     * Creates a new effect by lifting an effect's errors into a value.
     *
     * The resulting effect cannot fail.
     *
     * @return effectful error or the original value
     */
    def either(implicit system: EffectSystem[Effect]): Effect[Either[Throwable, T]] =
      system.either(effect)

    /**
     * Creates a new effect by applying a function to an effect's value.
     *
     * @param function function applied to the specified effect's value
     * @tparam R function result type
     * @return transformed effectful value
     */
    def map[R](function: T => R)(implicit system: EffectSystem[Effect]): Effect[R] =
      system.map(effect)(function)

    /**
     * Creates a new effect by applying an effectful function to an effect's value.
     *
     * @param function effectful function applied to the specified effect's value
     * @tparam R effectful function result type
     * @return effect containing the transformed value
     */
    def flatMap[R](function: T => Effect[R])(implicit system: EffectSystem[Effect]): Effect[R] =
      system.flatMap(effect)(function)

    /**
     * Executes an effect asynchronously without blocking.
     *
     * @return nothing
     */
    def run(implicit system: EffectSystem[Effect]): Unit =
      system.run(effect)
  }

  implicit class BinaryOps(data: Binary) {
    def toArray: Array[Byte] = data.unsafeArray

    def toByteBuffer: ByteBuffer = ByteBuffer.wrap(data.toArray)

    def toInputStream: InputStream = new ByteArrayInputStream(data.toArray)

    def asString: String = new String(data.toArray, charset)
  }

  implicit class ByteArrayOps(data: Array[Byte]) {
    def toBinary: Binary = new ArraySeq.ofByte(data)

    def toBinary(length: Int): Binary = data.take(length).toBinary
  }

  implicit class StringOps(data: String) {
    def toBinary: Binary = data.getBytes(charset).toBinary

    def toBinary(length: Int): Binary = data.getBytes(charset).toBinary(length)
  }

  implicit class ByteBufferOps(data: ByteBuffer) {
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

  implicit class InputStreamOps(data: InputStream) {
    /** Input stream reading buffer size. */
    private val bufferSize = 4096

    def toBinary: Binary = {
      val outputStream = new ByteArrayOutputStream()
      val buffer = Array.ofDim[Byte](bufferSize)
      LazyList.iterate(0) { case _ =>
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
      LazyList.iterate(length) { case remaining =>
        data.read(buffer, 0, Math.min(remaining, buffer.length)) match {
          case length if length >= 0 =>
            outputStream.write(buffer, 0, length)
            remaining - length
          case _ => 0
        }
      }.takeWhile(_ > 0).lastOption
      new ArraySeq.ofByte(outputStream.toByteArray)
    }
  }
}
