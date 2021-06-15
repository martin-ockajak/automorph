package test.backend

import jsonrpc.backend.MonixBackend
import jsonrpc.spi.Backend
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try
import test.backend.BackendSpec

class MonixBackendSpec extends BackendSpec[Task] {
  def effect: Backend[Task] = MonixBackend()

  def run[T](effect: Task[T]): Either[Throwable, T] = Try(effect.runSyncUnsafe(Duration.Inf)).toEither
}
