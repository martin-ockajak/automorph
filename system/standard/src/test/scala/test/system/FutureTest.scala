package test.system

import automorph.system.FutureSystem
import automorph.spi.EffectSystem
import automorph.spi.system.Defer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class FutureTest extends DeferEffectSystemTest[Future] {

  def deferSystem: EffectSystem[Future] with Defer[Future] = FutureSystem()

  def run[T](effect: Future[T]): Either[Throwable, T] =
    Try(await(effect)).toEither
}
