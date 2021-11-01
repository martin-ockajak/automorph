//package test.transport.http
//
//import automorph.Types
//import automorph.spi.EffectSystem
//import automorph.spi.system.Defer
//import automorph.spi.transport.ServerMessageTransport
//import automorph.system.FutureSystem
//import automorph.transport.http.endpoint.JettyEndpoint
//import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
//import org.eclipse.jetty.server.{Request, Server}
//import org.eclipse.jetty.server.handler.AbstractHandler
//import org.eclipse.jetty.servlet.{ServletContextHandler, ServletHandler, ServletHolder}
//import org.scalacheck.Arbitrary
//import scala.collection.mutable.ArrayBuffer
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import test.standard.StandardHttpServerTest
//import test.transport.http.HttpContextGenerator
//
//class JettyHttpFutureTest extends StandardHttpServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = JettyEndpoint.Context
//
//  override lazy val deferSystem: EffectSystem[Effect] with Defer[Effect] = FutureSystem()
//  override lazy val arbitraryContext: Arbitrary[Context] = HttpContextGenerator.arbitrary
//
//  def serverTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context],
//    port: Int
//  ): ServerMessageTransport[Effect] = new ServerMessageTransport[Effect] {
//    private val server = {
//      val endpoint = JettyEndpoint.create(handler)(runEffect)
//      val server = new Server(port)
//      val servletHandler = new ServletContextHandler
//      servletHandler.setContextPath("/")
//      servletHandler.addServlet(new ServletHolder(endpoint), "/*")
//      server.setHandler(servletHandler)
////      server.setHandler(new AbstractHandler {
////        override def handle(target: String, baseRequest: Request,
////          request: HttpServletRequest, response: HttpServletResponse): Unit = {
////          baseRequest.setHandled(true)
////          response.setStatus(200)
////          response.setContentType("text/plain")
////          response.getWriter.print("{TEEEEEEEEEEEEEEST}")
////        }
////      })
//      server.start()
//      server
//    }
//
//    override def close(): Future[Unit] = Future(server.stop())
//  }
//
//  override def run[T](effect: Effect[T]): T = await(effect)
//
//  override def runEffect[T](effect: Effect[T]): Unit = ()
//}
