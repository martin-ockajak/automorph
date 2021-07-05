package test.backend

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import jsonrpc.backend.CatsBackend
import jsonrpc.spi.Backend
import scala.util.Try
import test.backend.BackendSpec

class CatsBackendSpec extends BackendSpec[IO] {
  def effect: Backend[IO] = CatsBackend()

  def run[T](effect: IO[T]): Either[Throwable, T] = Try(effect.unsafeRunSync()).toEither
}
