package test.transport.local

import automorph.system.MonixSystem
import automorph.spi.EffectSystem
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalacheck.Arbitrary
import scala.concurrent.duration.Duration
import test.core.ProtocolCodecTest

class LocalMonixTest extends ProtocolCodecTest {

  type Effect[T] = Task[T]
  type Context = String

  override lazy val system: EffectSystem[Effect] = MonixSystem()

  override def run[T](effect: Effect[T]): T =
    effect.runSyncUnsafe(Duration.Inf)

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
