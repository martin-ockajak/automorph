package test.local

import automorph.system.MonixSystem
import automorph.spi.EffectSystem
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalacheck.Arbitrary
import scala.concurrent.duration.Duration
import test.core.FormatCoreSpec

class MonixLocalSpec extends FormatCoreSpec {

  type Effect[T] = Task[T]
  type Context = String

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val system: EffectSystem[Effect] = MonixSystem()

  override def run[T](effect: Effect[T]): T = effect.runSyncUnsafe(Duration.Inf)
}
