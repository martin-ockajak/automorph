package test.transport.local

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import automorph.system.CatsEffectSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.core.ProtocolCodecTest

class LocalCatsEffectTest extends ProtocolCodecTest {

  type Effect[T] = IO[T]
  type Context = String

  override lazy val system: EffectSystem[Effect] =
    CatsEffectSystem()

  override def execute[T](effect: Effect[T]): T =
    effect.unsafeRunSync()

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
