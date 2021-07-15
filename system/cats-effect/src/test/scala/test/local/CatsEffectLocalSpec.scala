package test.local

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import automorph.system.CatsEffectSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.FormatCoreSpec

class CatsEffectLocalSpec extends FormatCoreSpec {

  type Effect[T] = IO[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: EffectSystem[Effect] = CatsEffectSystem()

  override def run[T](effect: Effect[T]): T = effect.unsafeRunSync()
}
