package examples.transport

import automorph.{Client, Default, Server}
import automorph.transport.amqp.client.RabbitMqClient
import automorph.transport.amqp.server.RabbitMqServer
import io.arivera.oss.embedded.rabbitmq.{EmbeddedRabbitMq, EmbeddedRabbitMqConfig}
import java.net.URI
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
    if (Try(Process("erl -eval 'halt()' -noshell").! == 0).getOrElse(false)) {

      // Define a helper function to evaluate Futures
      def run[T](effect: Future[T]): T = Await.result(effect, Duration.Inf)

      // Create server API instance
      class ServerApi {
        def hello(some: String, n: Int): Future[String] =
          Future(s"Hello $some $n!")
      }
      val api = new ServerApi()

      // Start embedded RabbitMQ broker
      val brokerConfig = new EmbeddedRabbitMqConfig.Builder().port(7000)
        .rabbitMqServerInitializationTimeoutInMillis(30000).build()
      val broker = new EmbeddedRabbitMq(brokerConfig)
      broker.start()

      // Create RabbitMQ AMQP server transport consuming requests from the 'api' queue
      val serverTransport = RabbitMqServer(Default.effectSystemAsync, new URI("amqp://localhost:7000"), Seq("api"))

      // Start RabbitMQ AMQP JSON-RPC server
      val server = run(
        Server.transport(serverTransport).rpcProtocol(Default.rpcProtocol).bind(api).init()
      )

      // Define client view of the remote API
      trait ClientApi {
        def hello(some: String, n: Int): Future[String]
      }

      // Create RabbitMQ AMQP client message transport publishing requests to the 'api' queue
      val clientTransport = RabbitMqClient(new URI("amqp://localhost:7000"), "api", Default.effectSystemAsync)

      // Setup JSON-RPC HTTP client
      val client = run(
        Client.transport(clientTransport).rpcProtocol(Default.rpcProtocol).init()
      )

      // Call the remote API function
      val remoteApi = client.bind[ClientApi]
      println(run(
        remoteApi.hello("world", 1)
      ))

      // Close the client
      run(client.close())

      // Stop the server
      run(server.close())

      // Stop embedded RabbitMQ broker
      broker.stop()
      val brokerDirectory = brokerConfig.getExtractionFolder.toPath.resolve(brokerConfig.getVersion.getExtractionFolder)
      Files.walk(brokerDirectory).iterator().asScala.toSeq.reverse.foreach(_.toFile.delete())
    } else {
      println("Missing Erlang installation")
    }
  }
}
