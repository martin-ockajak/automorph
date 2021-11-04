package test.transport.local

import automorph.system.TrySystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import scala.util.Try
import test.core.ProtocolCodecTest

class LocalTryTest extends ProtocolCodecTest {

  type Effect[T] = Try[T]
  type Context = Seq[Int]

  override lazy val system: EffectSystem[Effect] =
    TrySystem()

  override def execute[T](effect: Effect[T]): T =
    effect.get

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
