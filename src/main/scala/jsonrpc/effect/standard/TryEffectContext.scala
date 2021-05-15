package jsonrpc.effect.native

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.EffectContext
import scala.collection.immutable.ArraySeq
import scala.util.{Success, Failure, Try}

final case class TryEffectContext() 
  extends EffectContext[Try]:
  
  def unit[T](value: T): Try[T] = Success(value)

  def transform[T](result: Try[T], success: (T) => Unit, failure: (Throwable) => Unit): Try[Unit] =
    result match
      case Success(value) => success(value)
      case Failure(exception) => failure(exception)
    Success(())
