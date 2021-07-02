package test.backend

import jsonrpc.backend.IdentityBackend
import jsonrpc.backend.IdentityBackend.Identity
import jsonrpc.spi.Backend
import scala.util.Try
import test.backend.BackendSpec

class IdentityBackendSpec extends BackendSpec[Identity] {
  def effect: Backend[Identity] = IdentityBackend()

  def run[T](effect: Identity[T]): Either[Throwable, T] = Try(effect).toEither
}
