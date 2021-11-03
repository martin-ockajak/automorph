//package test.transport.http
//
//import automorph.Types
//import automorph.spi.transport.{ClientMessageTransport, ServerMessageTransport}
//import automorph.system.FutureSystem
//import automorph.transport.amqp.client.RabbitMqClient
//import automorph.transport.amqp.server.RabbitMqServer
//import java.net.{ServerSocket, URI}
//import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig, PredefinedVersion}
//import io.arivera.oss.embedded.rabbitmq.helpers.ErlangVersionChecker
//import org.scalacheck.Arbitrary
//import scala.concurrent.Future
//import scala.util.{Failure, Success, Try}
//import test.core.ClientServerTest
//import test.transport.amqp.AmqpContextGenerator
//
//class RabbitMqFutureTest extends ClientServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = RabbitMqServer.Context
//
//  private lazy val defaultPort = 5672
//  private lazy val queue = "Test"
//
//  override lazy val system: FutureSystem = FutureSystem()
//
//  override def arbitraryContext: Arbitrary[Context] =
//    AmqpContextGenerator.arbitrary
//
//  override def run[T](effect: Effect[T]): T = await(effect)
//
//  override def customTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context]
//  ): Option[ClientMessageTransport[Effect, Context]] = {
//    Option.when(brokerAvailable) {
//      val (server, port) = withAvailablePort(port =>
//        new ServerMessageTransport[Effect] {
//          private val server = {
//            val url = new URI(s"amqp://localhost:$port")
//            RabbitMqServer[Effect](handler, url, Seq(queue))
//          }
//
//          override def close(): Future[Unit] =
//            server.close()
//        } -> port
//      )
//      servers += server
//      val url = new URI(s"amqp://localhost:$port")
//      val client = RabbitMqClient[Effect](url, queue, system)
//      clients += client
//      client
//    }
//  }
//
//  private def brokerAvailable: Boolean = {
//    Try(new ServerSocket(defaultPort)) match {
//      case Success(socket) =>
//        socket.close()
//        false
//      case Failure(_) => true
//    }
//  }
//}
