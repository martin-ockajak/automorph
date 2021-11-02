package test.system

import automorph.system.ZioSystem
import scala.util.Try
import test.system.ZioTest.Effect
import zio.{RIO, ZEnv}

class ZioTest extends RunTest[Effect] with DeferTest[Effect] {

  def system: ZioSystem[ZEnv] = ZioSystem.default

  def execute[T](effect: RIO[ZEnv, T]): Either[Throwable, T] =
    Try(system.runtime.unsafeRunTask(effect)).toEither
}

object ZioTest {

  type Effect[T] = RIO[ZEnv, T]
}
