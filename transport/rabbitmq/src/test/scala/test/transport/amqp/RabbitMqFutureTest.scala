package test.transport.amqp

import automorph.Types
import automorph.spi.transport.ClientMessageTransport
import automorph.system.FutureSystem
import automorph.transport.amqp.client.RabbitMqClient
import automorph.transport.amqp.server.RabbitMqServer
import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig}
import java.net.{ServerSocket, URI}
import org.scalacheck.Arbitrary
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process.Process
import scala.util.{Failure, Success, Try}
import test.core.ClientServerTest

class RabbitMqFutureTest extends ClientServerTest {

  type Effect[T] = Future[T]
  type Context = RabbitMqServer.Context

  private lazy val queue = "test"
  private lazy val brokers = ArrayBuffer.empty[EmbeddedRabbitMq]

  override lazy val system: FutureSystem = FutureSystem()

  override def execute[T](effect: Effect[T]): T =
    await(effect)

  override def arbitraryContext: Arbitrary[Context] =
    AmqpContextGenerator.arbitrary

  override def clientTransport(
    handler: Types.HandlerAnyCodec[Effect, Context]
  ): Option[ClientMessageTransport[Effect, Context]] =
    Option.when(Try(Process("disabled_erl -eval 'halt()' -noshell").! == 0).getOrElse(false)) {
      val (broker, port) = withRandomAvailablePort{ port =>
        val broker = new EmbeddedRabbitMq(new EmbeddedRabbitMqConfig.Builder().port(port).build())
        broker.start()
        broker -> port
      }
      brokers += broker
      val url = new URI(s"amqp://localhost:$port")
      val server = RabbitMqServer[Effect](handler, url, Seq(queue))
      servers += server
      val client = RabbitMqClient[Effect](url, queue, system)
      clients += client
      client
    }

  override def afterAll(): Unit = {
    brokers.foreach(_.stop())
    super.afterAll()
  }
}
