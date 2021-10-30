package test.system

import automorph.spi.EffectSystem
import automorph.spi.system.Defer
import automorph.system.ZioSystem
import scala.util.Try
import test.system.ZioTest.ZioEffect
import zio.{RIO, Runtime, ZEnv}

class ZioTest extends DeferEffectSystemTest[ZioEffect] {

  private lazy val runtime = Runtime.default.withReportFailure(_ => ())

  def deferSystem: EffectSystem[ZioEffect] with Defer[ZioEffect] =
    ZioSystem[ZEnv]()

  def run[T](effect: RIO[ZEnv, T]): Either[Throwable, T] = Try(runtime.unsafeRunTask(effect)).toEither
}

object ZioTest {

  type ZioEffect[T] = RIO[ZEnv, T]
}
