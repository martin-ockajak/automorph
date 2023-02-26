package examples.select

import automorph.Default
import automorph.transport.amqp.client.RabbitMqClient
import automorph.transport.amqp.server.RabbitMqServer
import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig}
import java.net.{ServerSocket, URI}
import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.sys.process.Process
import scala.util.Try

private[examples] object AmqpTransport {
  @scala.annotation.nowarn
  def main(arguments: Array[String]): Unit = {

    // Create server API instance
    class ServerApi {
      def hello(some: String, n: Int): Future[String] =
        Future(s"Hello $some $n!")
    }
    val api = new ServerApi()

    // Start embedded RabbitMQ broker if needed
    val noBroker = Try(new ServerSocket(5672)).fold(_ => false, socket => {
      socket.close()
      true
    })
    val embeddedBroker = Option.when(noBroker && Try(
      Process("erl -eval 'halt()' -noshell").! == 0
    ).getOrElse(false)) {
      val config = new EmbeddedRabbitMqConfig.Builder()
        .rabbitMqServerInitializationTimeoutInMillis(30000).build()
      val broker = new EmbeddedRabbitMq(config)
      broker.start()
      broker -> config
    }

    // Start RabbitMQ AMQP server consuming requests from the 'api' queue
    val handler = Default.handlerAsync[RabbitMqServer.Context]
    val server = RabbitMqServer(handler.bind(api), new URI("amqp://localhost"), Seq("api"))

    // Define client view of the remote API
    trait ClientApi {
      def hello(some: String, n: Int): Future[String]
    }

    // Create RabbitMQ AMQP client message transport publishing requests to the 'api' queue
    val transport = RabbitMqClient(new URI("amqp://localhost"), "api", Default.systemAsync)

    // Setup JSON-RPC HTTP client
    val client = Default.client(transport)

    // Call the remote API function
    val remoteApi = client.bind[ClientApi]
    println(Await.result(
      remoteApi.hello("world", 1),
      Duration.Inf
    ))

    // Close the client
    Await.result(client.close(), Duration.Inf)

    // Stop the server
    Await.result(server.close(), Duration.Inf)

    // Stop embedded RabbitMQ broker
    embeddedBroker.foreach { case (broker, config) =>
      broker.stop()
      val brokerDirectory = config.getExtractionFolder.toPath.resolve(config.getVersion.getExtractionFolder)
      Files.walk(brokerDirectory).iterator().asScala.toSeq.reverse.foreach(_.toFile.delete())
    }
  }
}
