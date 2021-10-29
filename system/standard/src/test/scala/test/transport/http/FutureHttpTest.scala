package test.transport.http

import automorph.Types
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
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.ProtocolCodecTest
import test.transport.http.HttpContextGenerator

class FutureHttpTest extends ProtocolCodecTest {

  type Effect[T] = Future[T]
  type Context = NanoServer.Context

  private lazy val servers = ArrayBuffer.empty[NanoServer[Effect]]

  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary

  override lazy val system: EffectSystem[Effect] = FutureSystem()

  override def run[T](effect: Effect[T]): T = await(effect)

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    val server = withAvailablePort(port => NanoServer.create[Effect](handler, port)(await))
    servers += server
    val url = new URI(s"http://localhost:${server.port}")
    Some(UrlClient(url, "POST", system).asInstanceOf[ClientMessageTransport[Effect, Context]])
  }

  override def afterAll(): Unit = {
    servers.foreach(_.close())
    super.afterAll()
  }
}
