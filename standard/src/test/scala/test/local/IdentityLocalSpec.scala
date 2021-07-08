package test.local

import automorph.backend.IdentityBackend
import automorph.backend.IdentityBackend.Identity
import automorph.spi.Backend
import org.scalacheck.Arbitrary
import test.CodecCoreSpec

class IdentityLocalSpec extends CodecCoreSpec {

  type Effect[T] = Identity[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: Backend[Effect] = IdentityBackend()

  override def run[T](effect: Effect[T]): T = effect
}
