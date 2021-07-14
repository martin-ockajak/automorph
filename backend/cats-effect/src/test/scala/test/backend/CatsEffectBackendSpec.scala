package test.backend

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import automorph.backend.CatsEffectBackend
import automorph.spi.Backend
import scala.util.Try

class CatsEffectBackendSpec extends BackendSpec[IO] {
  def effect: Backend[IO] = CatsEffectBackend()

  def run[T](effect: IO[T]): Either[Throwable, T] = Try(effect.unsafeRunSync()).toEither
}
