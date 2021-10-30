package test.system

import automorph.system.MonixSystem
import automorph.spi.EffectSystem
import automorph.spi.system.Defer
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.Try

class MonixTest extends DeferEffectSystemTest[Task] {

  def deferSystem: EffectSystem[Task] with Defer[Task] = MonixSystem()

  def run[T](effect: Task[T]): Either[Throwable, T] =
    Try(effect.runSyncUnsafe(Duration.Inf)).toEither
}
