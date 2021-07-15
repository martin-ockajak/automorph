package test.local

import automorph.system.TrySystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import scala.util.Try
import test.FormatCoreSpec

class TryLocalSpec extends FormatCoreSpec {

  type Effect[T] = Try[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override def run[T](effect: Effect[T]): T = effect.get

  override def backend: EffectSystem[Effect] = TrySystem()
}
