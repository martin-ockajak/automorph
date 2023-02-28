package test.transport.http

import automorph.Types
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.HttpClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.ClientServerTest

class HttpClientHttpFutureTest extends ClientServerTest {

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
    val url = new URI(s"http://localhost:${server.port}")
    val client = HttpClient(system, url, HttpMethod.Post).asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }

  override def execute[T](effect: Effect[T]): T =
    await(effect)
}
