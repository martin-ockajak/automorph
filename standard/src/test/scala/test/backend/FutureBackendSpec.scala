package test.backend

import automorph.backend.FutureBackend
import automorph.spi.Backend
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try
import test.backend.BackendSpec

class FutureBackendSpec extends BackendSpec[Future] {
  def effect: Backend[Future] = FutureBackend()

  def run[T](effect: Future[T]): Either[Throwable, T] = Try(await(effect)).toEither
}
