package test.transport.websocket

import automorph.spi.{ClientTransport, ServerTransport}
import automorph.system.FutureSystem
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.HttpClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.ClientServerTest
import test.transport.http.HttpContextGenerator

class HttpClientWebSocketFutureTest extends ClientServerTest {

  type Effect[T] = Future[T]
  type Context = NanoServer.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def clientTransport: ClientTransport[Effect, ?] =
    HttpClient(system, new URI(s"ws://localhost:$port"), HttpMethod.Get)

  override def serverTransport: ServerTransport[Effect, Context] =
    NanoServer[Effect](system, port)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def run[T](effect: Effect[T]): T =
    await(effect)
}
