package test.local

import automorph.system.MonixBackend
import automorph.spi.EffectSystem
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalacheck.Arbitrary
import scala.concurrent.duration.Duration
import test.FormatCoreSpec

class MonixLocalSpec extends FormatCoreSpec {

  type Effect[T] = Task[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: EffectSystem[Effect] = MonixBackend()

  override def run[T](effect: Effect[T]): T = effect.runSyncUnsafe(Duration.Inf)
}
