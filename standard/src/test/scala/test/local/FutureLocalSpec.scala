package test.local

import automorph.backend.FutureBackend
import automorph.spi.Backend
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.CodecCoreSpec

class FutureLocalSpec extends CodecCoreSpec {

  type Effect[T] = Future[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: Backend[Effect] = FutureBackend()

  override def run[T](effect: Effect[T]): T = await(effect)
}
