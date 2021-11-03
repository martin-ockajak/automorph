package test.system

import automorph.system.CatsEffectSystem
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.effect.unsafe.implicits.global
import scala.util.Try

class CatsEffectTest extends DeferEffectSystemTest[IO] {

  def system: CatsEffectSystem = CatsEffectSystem()

  def execute[T](effect: IO[T]): Either[Throwable, T] = {
    implicit val runtime: IORuntime = system.runtime
    Try(effect.unsafeRunSync()).toEither
  }
}
