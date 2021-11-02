package test.system

import automorph.system.ScalazSystem
import automorph.spi.EffectSystem
import scala.util.Try
import scalaz.effect.IO

class ScalazTest extends EffectSystemTest[IO] {
  def system: EffectSystem[IO] = ScalazSystem()

  def run[T](effect: IO[T]): Either[Throwable, T] =
    Try(effect.unsafePerformIO()).toEither
}
