package test.transport.local

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.core.ProtocolCodecTest

class LocalIdentityTest extends ProtocolCodecTest {

  type Effect[T] = Identity[T]
  type Context = Option[String]

  override lazy val system: EffectSystem[Effect] = IdentitySystem()

  override def run[T](effect: Effect[T]): T =
    effect

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
