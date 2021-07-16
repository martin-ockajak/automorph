package test.local

import automorph.system.FutureSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.FormatCoreSpec

class FutureLocalSpec extends FormatCoreSpec {

  type Effect[T] = Future[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val system: EffectSystem[Effect] = FutureSystem()

  override def run[T](effect: Effect[T]): T = await(effect)
}
