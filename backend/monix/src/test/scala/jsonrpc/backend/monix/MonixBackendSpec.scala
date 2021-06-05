package jsonrpc.backend.monix

import jsonrpc.backend.BackendSpec
import jsonrpc.spi.Backend
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

class MonixBackendSpec extends BackendSpec[Task] :
  def effect: Backend[Task] = MonixBackend()

  def run[T](outcome: Task[T]): Either[Throwable, T] = Try(outcome.runSyncUnsafe(Duration.Inf)).toEither
