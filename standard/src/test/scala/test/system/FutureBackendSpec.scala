package test.system

import automorph.system.FutureBackend
import automorph.spi.EffectSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class FutureBackendSpec extends BackendSpec[Future] {
  def effect: EffectSystem[Future] = FutureBackend()

  def run[T](effect: Future[T]): Either[Throwable, T] = Try(await(effect)).toEither
}
