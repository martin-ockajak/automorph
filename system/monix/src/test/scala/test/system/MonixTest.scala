package test.system

import automorph.system.MonixSystem
import monix.eval.Task
import monix.execution.Scheduler
import scala.concurrent.duration.Duration
import scala.util.Try

class MonixTest extends DeferEffectSystemTest[Task] {

  def system: MonixSystem = MonixSystem()

  def execute[T](effect: Task[T]): Either[Throwable, T] = {
    implicit val scheduler: Scheduler = system.scheduler
    Try(effect.runSyncUnsafe(Duration.Inf)).toEither
  }
}
