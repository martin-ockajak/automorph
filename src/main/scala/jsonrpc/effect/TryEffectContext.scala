package jsonrpc.effect

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.EffectContext
import scala.collection.immutable.ArraySeq
import scala.util.{Success, Try}

final case class TryEffectContext() extends EffectContext[Try]:
  def unit[T](value: T): Try[T] = Success(value)
