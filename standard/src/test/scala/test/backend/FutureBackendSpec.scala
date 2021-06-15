package test.backend

import jsonrpc.backend.FutureBackend
import jsonrpc.spi.Backend
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import test.backend.BackendSpec

class FutureBackendSpec extends BackendSpec[Future] {
  def effect: Backend[Future] = FutureBackend()

  def run[T](effect: Future[T]): Either[Throwable, T] = Try(await(effect)).toEither
}
