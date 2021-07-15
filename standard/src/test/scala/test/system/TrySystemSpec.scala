package test.system

import automorph.system.TrySystem
import automorph.spi.EffectSystem
import scala.util.Try

class TrySystemSpec extends SystemSpec[Try] {
  def system: EffectSystem[Try] = TrySystem()

  def run[T](effect: Try[T]): Either[Throwable, T] = effect.toEither
}
