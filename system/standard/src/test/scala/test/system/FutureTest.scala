package test.system

import automorph.system.FutureSystem
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class FutureTest extends RunTest[Future] with DeferTest[Future] {

  def system: FutureSystem = FutureSystem()

  def execute[T](effect: Future[T]): Either[Throwable, T] = {
    implicit val executionContext: ExecutionContext = system.executionContext
    Try(await(effect)).toEither
  }
}
