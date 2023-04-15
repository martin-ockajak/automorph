package test.transport.http

import automorph.system.FutureSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HttpClientHttpFutureTest extends HttpClientHttpTest {

  type Effect[T] = Future[T]

  override lazy val system: FutureSystem = FutureSystem()

  override def run[T](effect: Effect[T]): T =
    await(effect)
}
