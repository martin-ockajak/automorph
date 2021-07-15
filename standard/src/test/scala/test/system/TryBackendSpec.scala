package test.system

import automorph.system.TryBackend
import automorph.spi.EffectSystem
import scala.util.Try

class TryBackendSpec extends BackendSpec[Try] {
  def effect: EffectSystem[Try] = TryBackend()

  def run[T](effect: Try[T]): Either[Throwable, T] = effect.toEither
}