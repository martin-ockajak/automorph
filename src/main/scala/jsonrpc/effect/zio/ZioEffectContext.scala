package jsonrpc.effect.zio

import java.io.{ByteArrayOutputStream, InputStream, OutputStream}
import java.nio.ByteBuffer
import jsonrpc.spi.EffectContext
import scala.collection.immutable.ArraySeq
import scala.util.{Failure, Success, Try}
import zio.Task
import zio.RIO

final case class ZioEffectContext[Environment]()
  extends EffectContext[[T] =>> RIO[Environment, T]]:

  def pure[T](value: T): RIO[Environment, T] = RIO.succeed(value)

  def map[T, R](value: RIO[Environment, T], function: T => R): RIO[Environment, R] = value.map(function)

  def either[T](value: RIO[Environment, T]): RIO[Environment, Either[Throwable, T]] = value.either

object ZioEffectContext
