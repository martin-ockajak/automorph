package jsonrpc.backend.scalaz

import jsonrpc.backend.BackendSpec
import jsonrpc.spi.Backend
import scalaz.effect.IO
import scala.util.Try

class ScalazBackendSpec extends BackendSpec[IO]:
  def effect: Backend[IO] = ScalazBackend()

  def run[T](outcome: IO[T]): Either[Throwable, T] = Try(outcome.unsafePerformIO()).toEither
