package test.system

import automorph.system.ScalazEffectSystem
import automorph.spi.EffectSystem
import scala.util.Try
import scalaz.effect.IO

class ScalazEffectTest extends EffectSystemTest[IO] {
  def system: EffectSystem[IO] = ScalazEffectSystem()

  def execute[T](effect: IO[T]): Either[Throwable, T] =
    Try(effect.unsafePerformIO()).toEither
}
