package test.system

import automorph.system.MonixSystem
import automorph.spi.EffectSystem
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

class MonixSystemSpec extends SystemSpec[Task] {
  def effect: EffectSystem[Task] = MonixSystem()

  def run[T](effect: Task[T]): Either[Throwable, T] = Try(effect.runSyncUnsafe(Duration.Inf)).toEither
}
