package test.transport.http

import automorph.Types
import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
import automorph.system.FutureSystem
import automorph.transport.amqp.client.RabbitMqClient
import automorph.transport.amqp.server.RabbitMqServer
import java.net.{ServerSocket, URI}
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import test.core.ClientServerTest
import test.transport.amqp.AmqpContextGenerator

class RabbitMqFutureTest extends ClientServerTest {

  type Effect[T] = Future[T]
  type Context = RabbitMqServer.Context

  private lazy val defaultPort = 5672

  override lazy val system: FutureSystem = FutureSystem()

  override def execute[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    AmqpContextGenerator.arbitrary

  override def customTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] = {
    Option.when(brokerPortTaken) {
      val url = new URI(s"amqp://localhost:$defaultPort")
      val (server, queue) = withRandomAvailablePort(port =>
        new ServerMessageTransport[Effect] {
          private val server = RabbitMqServer[Effect](handler, url, Seq(port.toString))

          override def close(): Future[Unit] =
            server.close()
        } -> port.toString
      )
      servers += server
      val client = RabbitMqClient[Effect](url, queue, system)
      clients += client
      client
    }
  }

  private def brokerPortTaken: Boolean = {
    Try(new ServerSocket(defaultPort)) match {
      case Success(socket) =>
        socket.close()
        false
      case Failure(_) => true
    }
  }
}
