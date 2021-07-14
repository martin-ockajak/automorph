package test.local

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import automorph.backend.CatsEffectBackend
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.CodecCoreSpec

class CatsEffectLocalSpec extends CodecCoreSpec {

  type Effect[T] = IO[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: EffectSystem[Effect] = CatsEffectBackend()

  override def run[T](effect: Effect[T]): T = effect.unsafeRunSync()
}
