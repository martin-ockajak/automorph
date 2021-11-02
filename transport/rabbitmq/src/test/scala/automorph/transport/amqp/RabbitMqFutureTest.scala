//package test.transport.http
//
//import automorph.Types
//import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
//import automorph.system.FutureSystem
//import automorph.transport.amqp.client.RabbitMqClient
//import automorph.transport.amqp.server.RabbitMqServer
//import java.net.URI
//import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig, PredefinedVersion}
//import io.arivera.oss.embedded.rabbitmq.helpers.ErlangVersionChecker
//import org.scalacheck.Arbitrary
//import scala.concurrent.Future
//import scala.util.Try
//import test.core.ClientServerTest
//import test.transport.amqp.AmqpContextGenerator
//
//class RabbitMqFutureTest extends ClientServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = RabbitMqServer.Context
//
//  private lazy val builder = (new EmbeddedRabbitMqConfig.Builder).version(PredefinedVersion.V3_8_1)
//  private lazy val queue = "Test"
//
//  override lazy val arbitraryContext: Arbitrary[Context] = AmqpContextGenerator.arbitrary
//  override lazy val system: FutureSystem = FutureSystem()
//
//  override def run[T](effect: Effect[T]): T = await(effect)
//
//  override def customTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context]
//  ): Option[ClientMessageTransport[Effect, Context]] = {
//    Try(new ErlangVersionChecker(builder.build).check()).toOption.map { _ =>
//      val (server, port) = withAvailablePort(port =>
//        new ServerMessageTransport[Effect] {
//          private val (server, broker) = {
//            val config = builder.port(port).build
//            val broker = new EmbeddedRabbitMq(config)
//            broker.start()
//            val url = new URI(s"amqp://localhost:$port")
//            val server = RabbitMqServer[Effect](handler, url, Seq(queue))
//            server -> broker
//          }
//
//          override def close(): Future[Unit] =
//            server.close().map(_ => broker.stop())
//        } -> port
//      )
//      servers += server
//      val url = new URI(s"amqp://localhost:$port")
//      val client = RabbitMqClient[Effect](url, queue, system)
//      clients += client
//      client
//    }
//  }
//}
