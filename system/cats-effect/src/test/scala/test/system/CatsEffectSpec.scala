package test.system

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import automorph.system.CatsEffectSystem
import automorph.spi.EffectSystem
import scala.util.Try

class CatsEffectSpec extends EffectSystemSpec[IO] {
  def system: EffectSystem[IO] = CatsEffectSystem()

  def run[T](effect: IO[T]): Either[Throwable, T] = Try(effect.unsafeRunSync()).toEither
}
