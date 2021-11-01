//package test.transport.http
//
//import automorph.Types
//import automorph.spi.EffectSystem
//import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
//import automorph.system.FutureSystem
//import automorph.transport.amqp.client.RabbitMqClient
//import automorph.transport.amqp.server.RabbitMqServer
//import java.net.URI
//import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig}
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import test.core.ClientServerTest
//import test.transport.amqp.AmqpContextGenerator
//
//class RabbitMqFutureTest extends ClientServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = RabbitMqServer.Context
//
//  private lazy val queue = "Test"
//  private lazy val deferSystem = FutureSystem()
//
//  override lazy val arbitraryContext: Arbitrary[Context] = AmqpContextGenerator.arbitrary
//  override lazy val system: EffectSystem[Effect] = deferSystem
//
//  override def run[T](effect: Effect[T]): T = await(effect)
//
//  override def runEffect[T](effect: Effect[T]): Unit = ()
//
//  override def customTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context]
//  ): Option[ClientMessageTransport[Effect, Context]] = {
//    val (server, port) = withAvailablePort(port =>
//      new ServerMessageTransport[Effect] {
//        private val (server, broker) = {
//          val config = (new EmbeddedRabbitMqConfig.Builder).port(port).build
//          val broker = new EmbeddedRabbitMq(config)
//          broker.start()
//          val url = new URI(s"amqp://localhost:$port")
//          val server = RabbitMqServer.create[Effect](handler, url, Seq(queue))(runEffect)
//          server -> broker
//        }
//
//        override def close(): Future[Unit] =
//          server.close().map(_ => broker.stop())
//      } -> port
//    )
//    servers += server
//    val url = new URI(s"amqp://localhost:$port")
//    val client = RabbitMqClient.create[Effect](url, queue, deferSystem)(runEffect)
//    clients += client
//    Some(client)
//  }
//}
