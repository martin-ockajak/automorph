package test.transport.local

import automorph.system.ScalazEffectSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import scalaz.effect.IO
import test.core.ProtocolCodecTest

class LocalScalazEffectTest extends ProtocolCodecTest {

  type Effect[T] = IO[T]
  type Context = String

  override lazy val arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val system: EffectSystem[Effect] =
    ScalazEffectSystem()

  override def run[T](effect: Effect[T]): T =
    effect.unsafePerformIO()
}
