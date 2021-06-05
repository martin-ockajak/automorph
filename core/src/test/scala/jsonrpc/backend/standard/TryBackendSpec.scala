package jsonrpc.backend.standard

import jsonrpc.backend.BackendSpec
import jsonrpc.spi.Backend
import scala.util.{Failure, Success, Try}

class TryBackendSpec extends BackendSpec[Try]:
  def effect: Backend[Try] = TryBackend()

  def run[T](outcome: Try[T]): Either[Throwable, T] = outcome.toEither
