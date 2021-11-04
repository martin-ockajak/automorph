package test.transport.local

import automorph.spi.EffectSystem
import automorph.system.FutureSystem
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.ProtocolCodecTest

class LocalFutureTest extends ProtocolCodecTest {

  type Effect[T] = Future[T]
  type Context = Map[String, Double]

  override lazy val system: EffectSystem[Effect] =
    FutureSystem()

  override def run[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
