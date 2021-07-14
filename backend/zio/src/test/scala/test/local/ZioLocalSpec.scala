package test.local

import automorph.backend.ZioBackend
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.CodecCoreSpec
import zio.{RIO, Runtime, ZEnv}

class ZioLocalSpec extends CodecCoreSpec {

  private lazy val runtime = Runtime.default.withReportFailure(_ => ())

  type Effect[T] = RIO[ZEnv, T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: EffectSystem[Effect] = ZioBackend[ZEnv]()

  override def run[T](effect: Effect[T]): T = runtime.unsafeRunTask(effect)
}
