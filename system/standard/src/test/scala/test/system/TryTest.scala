package test.system

import automorph.system.TrySystem
import automorph.spi.EffectSystem
import scala.util.Try

class TryTest extends EffectSystemTest[Try] {
  def system: EffectSystem[Try] =
    TrySystem()

  def execute[T](effect: Try[T]): Either[Throwable, T] =
    effect.toEither
}
