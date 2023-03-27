package test.transport.http

import automorph.spi.ServerTransport
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.JettyHttpEndpoint
import automorph.transport.http.server.JettyServer
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.standard.StandardHttpServerTest

class JettyServerHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = JettyHttpEndpoint.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def run[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport(id: Int): ServerTransport[Effect, Context] = {
    System.setProperty("org.eclipse.jetty.LEVEL", "ERROR")
    JettyServer(system, port(id))
  }
}
