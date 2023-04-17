package test.transport.http

import automorph.spi.{EffectSystem, EndpointTransport, ServerTransport}
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.UndertowHttpEndpoint
import automorph.transport.http.server.UndertowServer
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.standard.StandardHttpServerTest

class UndertowServerHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = UndertowServer.Context

  override lazy val system: EffectSystem[Effect] = FutureSystem()

  override def run[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport(id: Int): ServerTransport[Effect, Context] =
    UndertowServer[Effect](system, port(id))

  override def endpointTransport: EndpointTransport[Future, Context, ?] =
    UndertowHttpEndpoint(system)

  override def portRange: Range =
    Range(30000, 35000)
}
