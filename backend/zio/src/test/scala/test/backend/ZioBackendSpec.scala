package test.backend

import jsonrpc.backend.ZioBackend
import jsonrpc.spi.Backend
import scala.util.Try
import test.backend.BackendSpec
import zio.{RIO, Runtime, ZEnv}

class ZioBackendSpec extends BackendSpec[({ type Effect[T] = RIO[ZEnv, T] })#Effect] {
  def effect: Backend[({ type Effect[T] = RIO[ZEnv, T] })#Effect] = ZioBackend[ZEnv]()

  def run[T](outcome: RIO[ZEnv, T]): Either[Throwable, T] = Try(Runtime.default.unsafeRunTask(outcome)).toEither
}
