package test.transport.local

import automorph.spi.EffectSystem
import automorph.system.ScalazEffectSystem
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scalaz.effect.IO
import test.core.ProtocolCodecTest

class LocalScalazEffectTest extends ProtocolCodecTest {

  type Effect[T] = IO[T]
  type Context = String

  override lazy val system: EffectSystem[Effect] =
    ScalazEffectSystem()

  override def execute[T](effect: Effect[T]): T =
    effect.unsafePerformIO()

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
