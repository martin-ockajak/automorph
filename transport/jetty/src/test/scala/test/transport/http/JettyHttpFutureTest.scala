package test.transport.http

import automorph.Types
import automorph.spi.transport.ServerMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.JettyEndpoint
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHolder}
import org.scalacheck.Arbitrary
import scala.concurrent.Future
import test.standard.StandardHttpServerTest
import test.transport.http.HttpContextGenerator

class JettyHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = JettyEndpoint.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport(
    handler: Types.HandlerAnyCodec[Effect, Context],
    port: Int
  ): ServerMessageTransport[Effect] = new ServerMessageTransport[Effect] {
    private val server = {
      val endpoint = JettyEndpoint(handler)
      val servletHandler = new ServletContextHandler
      servletHandler.addServlet(new ServletHolder(endpoint), "/*")
      val server = new Server(port)
      server.setHandler(servletHandler)
      server.start()
      server
    }

    override def close(): Effect[Unit] =
      system.wrap(server.stop())
  }

  override def run[T](effect: Effect[T]): T = await(effect)
}
