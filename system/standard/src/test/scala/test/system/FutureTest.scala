package test.system

import automorph.system.FutureSystem
import scala.concurrent.Future
import scala.util.Try

class FutureTest extends DeferEffectSystemTest[Future] {

  def system: FutureSystem = FutureSystem()

  def execute[T](effect: Future[T]): Either[Throwable, T] =
    Try(await(effect)).toEither
}
