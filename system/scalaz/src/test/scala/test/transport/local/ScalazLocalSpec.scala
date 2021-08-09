package test.transport.local

import automorph.system.ScalazSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import scala.util.Try
import scalaz.effect.IO
import test.core.ProtocolCodecSpec

class ScalazLocalSpec extends ProtocolCodecSpec {

  type Effect[T] = IO[T]
  type Context = String

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val system: EffectSystem[Effect] = ScalazSystem()

  override def run[T](effect: Effect[T]): T = effect.unsafePerformIO()
}
