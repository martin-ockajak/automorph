package jsonrpc.backend.standard

import jsonrpc.backend.BackendSpec
import jsonrpc.spi.Backend
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class FutureBackendSpec extends BackendSpec[Future]:
  def effect: Backend[Future] = FutureBackend()

  def run[T](outcome: Future[T]): Either[Throwable, T] = Try(outcome.await).toEither
