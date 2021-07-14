package test.local

import automorph.system.IdentityBackend
import automorph.system.IdentityBackend.Identity
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.FormatCoreSpec

class IdentityLocalSpec extends FormatCoreSpec {

  type Effect[T] = Identity[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: EffectSystem[Effect] = IdentityBackend()

  override def run[T](effect: Effect[T]): T = effect
}
