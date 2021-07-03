package test.backend.local

import jsonrpc.backend.MonixBackend
import jsonrpc.spi.Backend
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalacheck.Arbitrary
import scala.concurrent.duration.Duration
import test.CodecClientHandlerSpec

class MonixLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = Task[T]
  type Context = Short

  override def run[T](effect: Effect[T]): T = effect.runSyncUnsafe(Duration.Inf)

  override lazy val backend: Backend[Effect] = MonixBackend()

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
}
