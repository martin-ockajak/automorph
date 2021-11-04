package test.system

import automorph.system.FutureSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class FutureTest extends DeferEffectSystemTest[Future] {

  lazy val system: FutureSystem = FutureSystem()

  def execute[T](effect: Future[T]): Either[Throwable, T] =
    Try(await(effect)).toEither
}
