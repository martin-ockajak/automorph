package test.local

import automorph.system.ScalazSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import scala.util.Try
import scalaz.effect.IO
import test.FormatCoreSpec

class ScalazLocalSpec extends FormatCoreSpec {

  type Effect[T] = IO[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val system: EffectSystem[Effect] = ScalazSystem()

  override def run[T](effect: Effect[T]): T = effect.unsafePerformIO()
}
