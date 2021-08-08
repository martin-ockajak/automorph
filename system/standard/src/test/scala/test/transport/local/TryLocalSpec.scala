package test.transport.local

import automorph.system.TrySystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import scala.util.Try
import test.core.ProtocolFormatSpec

class TryLocalSpec extends ProtocolFormatSpec {

  type Effect[T] = Try[T]
  type Context = Seq[Int]

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override def run[T](effect: Effect[T]): T = effect.get

  override def system: EffectSystem[Effect] = TrySystem()
}
