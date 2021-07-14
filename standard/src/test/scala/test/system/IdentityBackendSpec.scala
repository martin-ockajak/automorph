package test.system

import automorph.system.IdentityBackend
import automorph.system.IdentityBackend.Identity
import automorph.spi.EffectSystem
import scala.util.Try

class IdentityBackendSpec extends BackendSpec[Identity] {
  def effect: EffectSystem[Identity] = IdentityBackend()

  def run[T](effect: Identity[T]): Either[Throwable, T] = Try(effect).toEither
}
