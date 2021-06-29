package test.backend

import jsonrpc.backend.NoBackend
import jsonrpc.backend.NoBackend.Identity
import jsonrpc.spi.Backend
import scala.util.Try
import test.backend.BackendSpec

class NoBackendSpec extends BackendSpec[Identity] {
  def effect: Backend[Identity] = NoBackend()

  def run[T](effect: Identity[T]): Either[Throwable, T] = Try(effect).toEither
}
