package test.transport.http

import automorph.spi.{EndpointTransport, ServerTransport}
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.AkkaHttpEndpoint
import automorph.transport.http.server.AkkaServer
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.standard.StandardHttpServerTest

class AkkaServerHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = AkkaServer.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def run[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport(id: Int): ServerTransport[Effect, Context] = {
    AkkaServer(system, port(id))
  }

  override def endpointTransport: EndpointTransport[Future, Context, ?] =
    AkkaHttpEndpoint(system)
}
