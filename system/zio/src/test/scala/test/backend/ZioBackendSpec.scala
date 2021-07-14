package test.backend

import automorph.backend.ZioBackend
import automorph.spi.EffectSystem
import scala.util.Try
import zio.{RIO, Runtime, ZEnv}

class ZioBackendSpec extends BackendSpec[({ type Effect[T] = RIO[ZEnv, T] })#Effect] {
  private lazy val runtime = Runtime.default.withReportFailure(_ => ())

  def effect: EffectSystem[({ type Effect[T] = RIO[ZEnv, T] })#Effect] = ZioBackend[ZEnv]()

  def run[T](effect: RIO[ZEnv, T]): Either[Throwable, T] = Try(runtime.unsafeRunTask(effect)).toEither
}
