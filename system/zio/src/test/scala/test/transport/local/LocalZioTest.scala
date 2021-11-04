package test.transport.local

import automorph.system.ZioSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.core.ProtocolCodecTest
import zio.{RIO, Runtime, ZEnv}

class LocalZioTest extends ProtocolCodecTest {

  private lazy val runtime = Runtime.default.withReportFailure(_ => ())

  type Effect[T] = RIO[ZEnv, T]
  type Context = String

  override lazy val system: EffectSystem[Effect] =
    ZioSystem[ZEnv]()(ZioSystem.defaultRuntime)

  override def run[T](effect: Effect[T]): T =
    runtime.unsafeRunTask(effect)

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
