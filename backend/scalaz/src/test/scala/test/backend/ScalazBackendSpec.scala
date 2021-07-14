package test.backend

import automorph.backend.ScalazBackend
import automorph.spi.EffectSystem
import scala.util.Try
import scalaz.effect.IO

class ScalazBackendSpec extends BackendSpec[IO] {
  def effect: EffectSystem[IO] = ScalazBackend()

  def run[T](effect: IO[T]): Either[Throwable, T] = Try(effect.unsafePerformIO()).toEither
}
