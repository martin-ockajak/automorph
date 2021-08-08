package test.transport.local

import automorph.system.IdentitySystem
import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.core.ProtocolFormatSpec

class IdentityLocalSpec extends ProtocolFormatSpec {

  type Effect[T] = Identity[T]
  type Context = Option[String]

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val system: EffectSystem[Effect] = IdentitySystem()

  override def run[T](effect: Effect[T]): T = effect
}
