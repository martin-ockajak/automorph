package test.system

import automorph.system.CatsEffectSystem
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import scala.util.Try

class CatsEffectTest extends CompletableEffectSystemTest[IO] {

  lazy val system: CatsEffectSystem = CatsEffectSystem()

  def execute[T](effect: IO[T]): Either[Throwable, T] =
    Try(effect.unsafeRunSync()).toEither
}
