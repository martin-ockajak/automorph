package jsonrpc.backend.cats

import jsonrpc.backend.BackendSpec
import jsonrpc.spi.Backend
import scala.util.Try
import cats.effect.IO
import cats.effect.unsafe.implicits.global

class CatsBackendSpec extends BackendSpec[IO] :
  def effect: Backend[IO] = CatsBackend()

  def run[T](outcome: IO[T]): Either[Throwable, T] = Try(outcome.unsafeRunSync()).toEither
