package test.system

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import automorph.system.CatsEffectSystem
import automorph.spi.EffectSystem
import automorph.spi.system.Defer
import scala.util.Try

class CatsEffectTest extends DeferEffectSystemTest[IO] {

  def deferSystem: EffectSystem[IO] with Defer[IO] = CatsEffectSystem()

  def run[T](effect: IO[T]): Either[Throwable, T] =
    Try(effect.unsafeRunSync()).toEither
}
