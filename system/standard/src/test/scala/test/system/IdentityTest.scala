package test.system

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem
import scala.util.Try

class IdentityTest extends EffectSystemTest[Identity] {
  def system: EffectSystem[Identity] =
    IdentitySystem()

  def execute[T](effect: Identity[T]): Either[Throwable, T] =
    Try(effect).toEither
}
