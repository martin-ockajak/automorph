package jsonrpc.effect.native

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.EffectContext
import scala.collection.immutable.ArraySeq
import scala.util.{Success, Failure, Try}

final case class TryEffectContext() extends EffectContext[Try]:
  
  def pure[T](value: T): Try[T] = Success(value)

  def map[T, R](value: Try[T], function: T => R): Try[R] = value.map(function)

  def either[T](value: Try[T]): Try[Either[Throwable, T]] = Success(value.toEither)
