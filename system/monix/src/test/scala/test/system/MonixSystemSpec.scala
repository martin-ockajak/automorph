package test.system

import automorph.system.MonixSystem
import automorph.spi.EffectSystem
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

class MonixSystemSpec extends EffectSystemSpec[Task] {
  def system: EffectSystem[Task] = MonixSystem()

  def run[T](effect: Task[T]): Either[Throwable, T] = Try(effect.runSyncUnsafe(Duration.Inf)).toEither
}
