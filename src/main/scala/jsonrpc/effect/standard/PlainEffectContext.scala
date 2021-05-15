package jsonrpc.effect.native

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.EffectContext
import scala.collection.immutable.ArraySeq
import scala.util.control.NonFatal
import scala.util.{Success, Try}

final case class PlainEffectContext() 
  extends EffectContext[PlainEffectContext.Id]:
  
  def unit[T](value: T): T = value

  def transform[T](result: T, success: (T) => Unit, failure: (Throwable) => Unit): Unit =
    try
      success(result)
    catch
      case NonFatal(e) => failure(e)

object PlainEffectContext:
  type Id[T] = T
