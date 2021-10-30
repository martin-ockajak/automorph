package test.transport.local

import automorph.system.ZioSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.core.ProtocolCodecTest
import zio.{RIO, Runtime, ZEnv}

class ZioLocalTest extends ProtocolCodecTest {

  private lazy val runtime = Runtime.default.withReportFailure(_ => ())

  type Effect[T] = RIO[ZEnv, T]
  type Context = String

  override lazy val arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val system: EffectSystem[Effect] =
    ZioSystem[ZEnv]()

  override def run[T](effect: Effect[T]): T =
    runtime.unsafeRunTask(effect)
}