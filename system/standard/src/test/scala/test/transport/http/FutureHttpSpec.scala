package test.transport.http

import automorph.spi.EffectSystem
import automorph.system.FutureSystem
import automorph.transport.http.Http
import automorph.transport.http.server.NanoHTTPD.IHTTPSession
import automorph.transport.http.server.{NanoHTTPD, NanoHttpdServer}
import java.io.InputStream
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.core.FormatCoreSpec
import test.transport.http.Generator

class FutureHttpSpec extends FormatCoreSpec {

  type Effect[T] = Future[T]
  type Context = NanoHttpdServer.Context

  private lazy val servers = fixtures.map { fixture =>
    NanoHttpdServer[Effect](fixture.handler, await, availablePort)
  }

  override lazy val arbitraryContext: Arbitrary[Context] = Generator.context

  override lazy val system: EffectSystem[Effect] = FutureSystem()

  override def run[T](effect: Effect[T]): T = await(effect)

  override def afterAll(): Unit = {
    servers.foreach(_.close())
    super.afterAll()
  }
}
