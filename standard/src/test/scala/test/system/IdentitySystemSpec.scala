package test.system

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem
import scala.util.Try

class IdentitySystemSpec extends SystemSpec[Identity] {
  def effect: EffectSystem[Identity] = IdentitySystem()

  def run[T](effect: Identity[T]): Either[Throwable, T] = Try(effect).toEither
}
