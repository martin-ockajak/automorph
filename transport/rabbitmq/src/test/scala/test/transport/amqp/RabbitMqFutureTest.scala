//package test.transport.amqp
//
//import automorph.Types
//import automorph.spi.transport.ClientMessageTransport
//import automorph.system.FutureSystem
//import automorph.transport.amqp.client.RabbitMqClient
//import automorph.transport.amqp.server.RabbitMqServer
//import io.arivera.oss.embedded.rabbitmq.apache.commons.lang3.SystemUtils
//import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig}
//
//import java.net.URI
//import java.nio.file.{Files, Paths}
//import scala.jdk.CollectionConverters.IteratorHasAsScala
//import org.scalacheck.Arbitrary
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import scala.sys.process.Process
//import scala.util.Try
//import test.core.ClientServerTest
//
//class RabbitMqFutureTest extends ClientServerTest {
//
//  type Effect[T] = Future[T]
//  type Context = RabbitMqServer.Context
//
//  private lazy val setupTimeout = 30000
//  private lazy val embeddedBroker = createBroker()
//
//  override lazy val system: FutureSystem = FutureSystem()
//
//  override def execute[T](effect: Effect[T]): T =
//    await(effect)
//
//  override def arbitraryContext: Arbitrary[Context] =
//    AmqpContextGenerator.arbitrary
//
//  override def clientTransport(
//    handler: Types.HandlerAnyCodec[Effect, Context]
//  ): Option[ClientMessageTransport[Effect, Context]] =
//    embeddedBroker match {
//      case Some((_, config)) => Some {
//        val protocol = handler.asInstanceOf[Types.HandlerGenericCodec[Effect, Context]].protocol
//        val queue = s"${protocol.name}/${protocol.codec.getClass.getName}"
//        val url = new URI(s"amqp://localhost:${config.getRabbitMqPort}")
//        val server = RabbitMqServer[Effect](handler, url, Seq(queue))
//        servers += server
//        val client = RabbitMqClient[Effect](url, queue, system)
//        clients += client
//        client
//      }
//      case _ => None
//    }
//
//  override def afterAll(): Unit = {
//    super.afterAll()
//    embeddedBroker.foreach { case (broker, config) =>
//      broker.stop()
//      val brokerDirectory = config.getExtractionFolder.toPath.resolve(config.getVersion.getExtractionFolder)
//      Files.walk(brokerDirectory).iterator().asScala.toSeq.reverse.foreach(_.toFile.delete())
//    }
//  }
//
//  private def createBroker(): Option[(EmbeddedRabbitMq, EmbeddedRabbitMqConfig)] = {
//    Option.when(Try(Process("erl -eval 'halt()' -noshell").! == 0).getOrElse(false)) {
//      val config = new EmbeddedRabbitMqConfig.Builder().randomPort()
//        .extractionFolder(Paths.get(SystemUtils.JAVA_IO_TMPDIR, getClass.getSimpleName).toFile)
//        .rabbitMqServerInitializationTimeoutInMillis(setupTimeout).build()
//      val broker = new EmbeddedRabbitMq(config)
//      broker.start()
//      broker -> config
//    }
//  }
//}
