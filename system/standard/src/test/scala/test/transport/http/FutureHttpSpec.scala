package test.transport.http

import automorph.spi.{ClientMessageTransport, EffectSystem}
import automorph.system.FutureSystem
import automorph.transport.http.Http
import automorph.transport.http.client.HttpUrlConnectionClient
import automorph.transport.http.server.NanoHTTPD.IHTTPSession
import automorph.transport.http.server.{NanoHTTPD, NanoHttpdServer}
import java.io.InputStream
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.FormatCoreSpec
import test.transport.http.Generator
import java.net.URI

class FutureHttpSpec extends FormatCoreSpec {

  type Effect[T] = Future[T]
  type Context = Http[_]

  private lazy val serverPorts = fixtures.map { fixture =>
    val port = availablePort
    NanoHttpdServer[Effect](fixture.handler, await, port) -> port
  }

  override lazy val arbitraryContext: Arbitrary[Context] = Generator.context

  override lazy val system: EffectSystem[Effect] = FutureSystem()

  override def run[T](effect: Effect[T]): T = await(effect)

  override def customTransport(index: Int): Option[ClientMessageTransport[Effect, Context]] = synchronized {
    println(s"CLIENT START $index")
//    val url = new URI(s"http://localhost:${serverPorts(index)._2}")
    println(s"CLIENT END $index")
    None
//    Some(HttpUrlConnectionClient(url, "POST", system).asInstanceOf[ClientMessageTransport[Effect, Context]])
  }

  override def afterAll(): Unit = {
    serverPorts.foreach { case (server, _) =>
      server.close()
    }
    super.afterAll()
  }
}
