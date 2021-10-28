package test.system

import automorph.system.TrySystem
import automorph.spi.EffectSystem
import scala.util.Try

class TrySpec extends EffectSystemSpec[Try] {
  def system: EffectSystem[Try] =
    TrySystem()

  def run[T](effect: Try[T]): Either[Throwable, T] =
    effect.toEither
}
