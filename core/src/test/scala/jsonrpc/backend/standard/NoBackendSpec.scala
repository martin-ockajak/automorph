package jsonrpc.backend.standard

import jsonrpc.backend.BackendSpec
import NoBackend.Identity
import jsonrpc.spi.Backend
import scala.util.{Failure, Success, Try}

class NoBackendSpec extends BackendSpec[Identity] :
  def effect: Backend[Identity] = NoBackend()

  def run[T](outcome: Identity[T]): Either[Throwable, T] = Try(outcome).toEither
