package test.transport.http

import automorph.Types
import automorph.spi.transport.ServerMessageTransport
import automorph.system.FutureSystem
import automorph.transport.http.endpoint.FinagleHttpEndpoint
import com.twitter.finagle.Http
import com.twitter.util.{Return, Throw}
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import test.standard.StandardHttpServerTest

class FinagleEndpointHttpFutureTest extends StandardHttpServerTest {

  type Effect[T] = Future[T]
  type Context = FinagleHttpEndpoint.Context

  override lazy val system: FutureSystem = FutureSystem()

  override def execute[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    HttpContextGenerator.arbitrary

  override def serverTransport(
    handler: Types.HandlerAnyCodec[Effect, Context],
    port: Int
  ): ServerMessageTransport[Effect] = new ServerMessageTransport[Effect] {
    private val server = {
      val endpoint = FinagleHttpEndpoint(handler)
      Http.serve(s":$port", endpoint)
    }

    override def close(): Effect[Unit] = {
      val promise = Promise[Unit]()
      server.close().respond {
        case Return(result) => promise.success(result)
        case Throw(error) => promise.failure(error)
      }
      promise.future
    }
  }
}
