package test.transport.http

import automorph.Types
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.TrySystem
import automorph.transport.http.client.UrlClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import scala.util.Try
import test.core.ClientServerTest
import test.transport.http.HttpContextGenerator

class TryUrlClientHttpTest extends ClientServerTest {

  type Effect[T] = Try[T]
  type Context = NanoServer.Context

  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary
  override lazy val system: EffectSystem[Effect] = TrySystem()

  override def run[T](effect: Effect[T]): T = effect.get

  override def runEffect[T](effect: Effect[T]): Unit = ()

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val server = withAvailablePort(port => NanoServer.create[Effect](handler, port)(_.get))
    servers += server
    val url = new URI(s"http://localhost:${server.port}")
    val client = UrlClient(url, "DELETE", system).asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }
}
