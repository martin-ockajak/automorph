package test.transport.http

import automorph.Types
import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.client.HttpClient
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.ClientServerTest
import test.transport.http.HttpContextGenerator

class FutureHttpTest extends ClientServerTest {

  type Effect[T] = Future[T]
  type Context = NanoServer.Context

  private lazy val deferSystem = FutureSystem()

  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary
  override lazy val system: EffectSystem[Effect] = deferSystem

  override def run[T](effect: Effect[T]): T = await(effect)

  override def runEffect[T](effect: Effect[T]): Unit = ()

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val server = withAvailablePort(port => NanoServer.create[Effect](handler, port)(await(_)))
    servers += server
    val url = new URI(s"http://localhost:${server.port}")
    val client = HttpClient.create(url, "POST", deferSystem)(runEffect)
      .asInstanceOf[ClientMessageTransport[Effect, Context]]
    clients += client
    Some(client)
  }
}
