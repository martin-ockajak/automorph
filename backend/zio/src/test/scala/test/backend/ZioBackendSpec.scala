package test.backend

import jsonrpc.backend.ZioBackend
import jsonrpc.spi.Backend
import scala.util.Try
import test.backend.BackendSpec
import zio.{RIO, Runtime, ZEnv}

class ZioBackendSpec extends BackendSpec[({ type Effect[T] = RIO[ZEnv, T] })#Effect] {
  private lazy val runtime = Runtime.default.withReportFailure(_ => ())

  def effect: Backend[({ type Effect[T] = RIO[ZEnv, T] })#Effect] = ZioBackend[ZEnv]()

  def run[T](effect: RIO[ZEnv, T]): Either[Throwable, T] = Try(runtime.unsafeRunTask(effect)).toEither
}
