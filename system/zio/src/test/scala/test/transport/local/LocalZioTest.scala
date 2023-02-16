package test.transport.local

import automorph.system.ZioSystem
import automorph.spi.EffectSystem
import org.scalacheck.Arbitrary
import test.core.ProtocolCodecTest
import zio.{RIO, Task, Unsafe}

class LocalZioTest extends ProtocolCodecTest {

  type Effect[T] = Task[T]
  type Context = String
  override lazy val system: EffectSystem[Effect] = ZioSystem[Any]()(ZioSystem.defaultRuntime)

  override def execute[T](effect: Effect[T]): T =
    Unsafe.unsafe { implicit unsafe =>
      ZioSystem.defaultRuntime.unsafe.run(effect).toEither.swap.map(_.getCause).swap.toTry.get
    }

  override def arbitraryContext: Arbitrary[Context] =
    Arbitrary(Arbitrary.arbitrary[Context])
}
