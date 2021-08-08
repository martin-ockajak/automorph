package test.transport.http

import automorph.spi.EffectSystem
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.Http
import automorph.transport.http.client.HttpUrlConnectionClient
import automorph.transport.http.server.NanoHttpdServer
import java.net.URI
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.ProtocolFormatSpec
import test.transport.http.Generator

class FutureHttpSpec extends ProtocolFormatSpec {

  type Effect[T] = Future[T]
  type Context = Http[_]

  private lazy val servers = fixtures.map { fixture =>
    NanoHttpdServer[Effect](fixture.handler, await, fixture.serverPort)
  }

  override lazy val arbitraryContext: Arbitrary[Context] = Generator.context

  override lazy val system: EffectSystem[Effect] = FutureSystem()

  override def run[T](effect: Effect[T]): T = await(effect)

  override def customTransport(port: Int): Option[ClientMessageTransport[Effect, Context]] = synchronized {
    val url = new URI(s"http://localhost:$port")
    Some(HttpUrlConnectionClient(url, "POST", system).asInstanceOf[ClientMessageTransport[Effect, Context]])
    None
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
