package test.backend

import jsonrpc.backend.ScalazBackend
import jsonrpc.spi.Backend
import scala.util.Try
import scalaz.effect.IO
import test.backend.BackendSpec

class ScalazBackendSpec extends BackendSpec[IO] {
  def effect: Backend[IO] = ScalazBackend()

  def run[T](effect: IO[T]): Either[Throwable, T] = Try(effect.unsafePerformIO()).toEither
}
