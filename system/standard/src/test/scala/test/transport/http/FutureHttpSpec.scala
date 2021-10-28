package test.transport.http

import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.HttpContext
import automorph.transport.http.client.UrlClient
import automorph.transport.http.server.NanoHTTPD.IHTTPSession
import automorph.transport.http.server.NanoServer
import java.net.URI
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.ProtocolCodecSpec
import test.transport.http.HttpContextGenerator

class FutureHttpSpec extends ProtocolCodecSpec {

  type Effect[T] = Future[T]
  type Context = NanoServer.Context

  private lazy val servers = fixtures.map { fixture =>
    NanoServer[Effect](fixture.handler, await, fixture.serverPort)
  }

  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary

  override lazy val system: EffectSystem[Effect] = FutureSystem()

  override def run[T](effect: Effect[T]): T = await(effect)

  override def customTransport(port: Int): Option[ClientMessageTransport[Effect, Context]] = synchronized {
    val url = new URI(s"http://localhost:$port")
//    val url = new URI(s"http://localhost:1234")
    Some(UrlClient(url, "POST", system).asInstanceOf[ClientMessageTransport[Effect, Context]])
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    servers
  }

  override def afterAll(): Unit = {
    servers.foreach(_.close())
    super.afterAll()
  }
}
