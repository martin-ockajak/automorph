package test.system

import ZioTest.ZioEffect
import automorph.system.ZioSystem
import automorph.spi.EffectSystem
import automorph.spi.system.Defer
import scala.util.Try
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
