package test.transport.websocket

import automorph.Types
import automorph.spi.ClientMessageTransport
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

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val server = withRandomAvailablePort(port => NanoServer[Effect](handler, port))
    servers += server
    val url = new URI(s"ws://localhost:${server.port}")
    val client = HttpClient(system, url, HttpMethod.Get).asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }

  override def execute[T](effect: Effect[T]): T =
    await(effect)
}
