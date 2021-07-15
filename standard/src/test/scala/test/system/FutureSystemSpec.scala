package test.system

import automorph.system.FutureSystem
import automorph.spi.EffectSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class FutureSystemSpec extends SystemSpec[Future] {
  def effect: EffectSystem[Future] = FutureSystem()

  def run[T](effect: Future[T]): Either[Throwable, T] = Try(await(effect)).toEither
}
