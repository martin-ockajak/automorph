package test.transport.local

import automorph.system.MonixSystem
import automorph.spi.EffectSystem
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.duration.Duration

class LocalMonixTest extends LocalTest {

  type Effect[T] = Task[T]

  override lazy val system: EffectSystem[Effect] =
    MonixSystem()

  override def run[T](effect: Effect[T]): T =
    effect.runSyncUnsafe(Duration.Inf)
}
