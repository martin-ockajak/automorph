package jsonrpc.effect.zio

import jsonrpc.effect.EffectSpec
import jsonrpc.spi.Effect
import scala.util.Try
import zio.Runtime
import zio.RIO
import zio.ZEnv
import zio.FiberFailure

class ZioEffectSpec extends EffectSpec[[T] =>> RIO[ZEnv, T]]:
  def effect: Effect[[T] =>> RIO[ZEnv, T]] = ZioEffect[ZEnv]()

  def run[T](outcome: RIO[ZEnv, T]): Either[Throwable, T] = Try(Runtime.default.unsafeRunTask(outcome)).toEither
