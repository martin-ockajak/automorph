package test.system

import automorph.system.ZioSystem
import automorph.spi.EffectSystem
import scala.util.Try
import zio.{RIO, Runtime, ZEnv}

class ZioTest extends EffectSystemTest[({ type Effect[T] = RIO[ZEnv, T] })#Effect] {
  private lazy val runtime = Runtime.default.withReportFailure(_ => ())

  def system: EffectSystem[({ type Effect[T] = RIO[ZEnv, T] })#Effect] = ZioSystem[ZEnv]()

  def run[T](effect: RIO[ZEnv, T]): Either[Throwable, T] = Try(runtime.unsafeRunTask(effect)).toEither
}
