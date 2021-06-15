package test.backend

import jsonrpc.backend.TryBackend
import jsonrpc.spi.Backend
import scala.util.{Failure, Success, Try}
import test.backend.BackendSpec

class TryBackendSpec extends BackendSpec[Try] {
  def effect: Backend[Try] = TryBackend()

  def run[T](effect: Try[T]): Either[Throwable, T] = effect.toEither
}
