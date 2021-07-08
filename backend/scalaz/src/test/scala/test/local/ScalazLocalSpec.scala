package test.local

import automorph.backend.ScalazBackend
import automorph.spi.Backend
import org.scalacheck.Arbitrary
import scala.util.Try
import scalaz.effect.IO
import test.CodecCoreSpec

class ScalazLocalSpec extends CodecCoreSpec {

  type Effect[T] = IO[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: Backend[Effect] = ScalazBackend()

  override def run[T](effect: Effect[T]): T = effect.unsafePerformIO()
}
