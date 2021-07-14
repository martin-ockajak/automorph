package test.backend

import automorph.backend.IdentityBackend
import automorph.backend.IdentityBackend.Identity
import automorph.spi.Backend
import scala.util.Try

class IdentityBackendSpec extends BackendSpec[Identity] {
  def effect: Backend[Identity] = IdentityBackend()

  def run[T](effect: Identity[T]): Either[Throwable, T] = Try(effect).toEither
}
