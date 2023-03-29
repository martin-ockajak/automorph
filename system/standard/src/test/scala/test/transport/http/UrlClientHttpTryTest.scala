package test.transport.http

import automorph.system.TrySystem
import scala.util.Try

class UrlClientHttpTryTest extends UrlClientHttpTest {

  type Effect[T] = Try[T]

  override lazy val system: TrySystem = TrySystem()

  override def run[T](effect: Effect[T]): T =
    effect.get
}
