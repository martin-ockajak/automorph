package test.transport.http

import automorph.Types
import automorph.spi.transport.ServerMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.JettyHttpEndpoint
import automorph.transport.http.server.JettyServer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.standard.StandardHttpServerTest
import test.transport.http.HttpContextGenerator

class JettyHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = JettyHttpEndpoint.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport(
    handler: Types.HandlerAnyCodec[Effect, Context],
    port: Int
  ): ServerMessageTransport[Effect] =
    JettyServer(handler, port)

  override def run[T](effect: Effect[T]): T = await(effect)
}
