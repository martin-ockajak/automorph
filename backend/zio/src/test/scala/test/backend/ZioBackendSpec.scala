package test.backend

import jsonrpc.backend.ZioBackend
import jsonrpc.spi.Backend
import scala.util.Try
import test.backend.BackendSpec
import zio.{FiberFailure, RIO, Runtime, ZEnv}

class ZioBackendSpec extends BackendSpec[[T] =>> RIO[ZEnv, T]] {
  def effect: Backend[[T] =>> RIO[ZEnv, T]] = ZioBackend[ZEnv]()

  def run[T](outcome: RIO[ZEnv, T]): Either[Throwable, T] = Try(Runtime.default.unsafeRunTask(outcome)).toEither
}
