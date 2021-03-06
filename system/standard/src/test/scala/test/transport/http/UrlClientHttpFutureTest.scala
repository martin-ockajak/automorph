package test.transport.http

import automorph.Types
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.HttpMethod
import automorph.transport.http.client.UrlClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.ClientServerTest

class UrlClientHttpFutureTest extends ClientServerTest {

  type Effect[T] = Future[T]
  type Context = NanoServer.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def execute[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def clientTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val server = withRandomAvailablePort(port => NanoServer.create[Effect](handler, port)(execute(_)))
    servers += server
    val url = new URI(s"http://localhost:${server.port}")
    val client = UrlClient(system, url, HttpMethod.Put).asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }
}
