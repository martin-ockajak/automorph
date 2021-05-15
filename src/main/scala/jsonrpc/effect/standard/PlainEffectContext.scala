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

  def map[T, R](value: T, function: T => R): R = function(value)

  def either[T](value: T): Either[Throwable, T] = Right(value)

object PlainEffectContext:
  type Id[T] = T
