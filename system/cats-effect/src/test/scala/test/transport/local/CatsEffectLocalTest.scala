package test.transport.local

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import automorph.system.CatsEffectSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.core.ProtocolCodecTest

class CatsEffectLocalTest extends ProtocolCodecTest {

  type Effect[T] = IO[T]
  type Context = String

  override lazy val arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val system: EffectSystem[Effect] =
    CatsEffectSystem()

  override def run[T](effect: Effect[T]): T =
    effect.unsafeRunSync()
}
