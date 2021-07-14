package test.local

import automorph.backend.TryBackend
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import scala.util.Try
import test.CodecCoreSpec

class TryLocalSpec extends CodecCoreSpec {

  type Effect[T] = Try[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override def run[T](effect: Effect[T]): T = effect.get

  override def backend: EffectSystem[Effect] = TryBackend()
}
