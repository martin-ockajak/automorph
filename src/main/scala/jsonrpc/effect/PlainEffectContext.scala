package jsonrpc.effect

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.EffectContext
import scala.collection.immutable.ArraySeq
import scala.util.{Success, Try}

final case class PlainEffectContext() 
  extends EffectContext[PlainEffectContext.Id]:
  
  def unit[T](value: T): T = value

object PlainEffectContext:
  type Id[T] = T
