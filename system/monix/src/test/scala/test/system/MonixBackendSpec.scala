package test.system

import automorph.system.MonixBackend
import automorph.spi.EffectSystem
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

class MonixBackendSpec extends BackendSpec[Task] {
  def effect: EffectSystem[Task] = MonixBackend()

  def run[T](effect: Task[T]): Either[Throwable, T] = Try(effect.runSyncUnsafe(Duration.Inf)).toEither
}
