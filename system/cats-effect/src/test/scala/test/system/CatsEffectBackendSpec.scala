package test.system

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import automorph.system.CatsEffectBackend
import automorph.spi.EffectSystem
import scala.util.Try

class CatsEffectBackendSpec extends BackendSpec[IO] {
  def effect: EffectSystem[IO] = CatsEffectBackend()

  def run[T](effect: IO[T]): Either[Throwable, T] = Try(effect.unsafeRunSync()).toEither
}