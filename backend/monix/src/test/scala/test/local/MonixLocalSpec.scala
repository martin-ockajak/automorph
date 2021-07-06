package test.local

import automorph.backend.MonixBackend
import automorph.spi.Backend
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalacheck.Arbitrary
import scala.concurrent.duration.Duration
import test.CodecClientHandlerSpec

class MonixLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = Task[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: Backend[Effect] = MonixBackend()

  override def run[T](effect: Effect[T]): T = effect.runSyncUnsafe(Duration.Inf)
}
