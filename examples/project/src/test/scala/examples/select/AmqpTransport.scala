package examples.select

import automorph.Default
import automorph.transport.amqp.client.RabbitMqClient
import automorph.transport.amqp.server.RabbitMqServer
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}

object AmqpTransport extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): String =
      s"Hello $some $n!"
  }
  val api = new ServerApi()

  // Start RabbitMQ AMQP server consuming requests from the 'api' queue
  val handler = Default.handlerAsync[RabbitMqServer.Context]
  val server = RabbitMqServer(handler.bind(api), new URI("amqp://localhost"), Seq("api"))

  // Define client view of the remote API
  trait ClientApi {
    def hello(some: String, n: Int): String
  }

  // Setup RabbitMQ AMQP client publishing requests to the 'api' queue
  val client = RabbitMqClient[Effect](new URI("amqp://localhost"), 'api')

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
}

class AmqpTransport extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      AmqpTransport.main(Array())
    }
  }
}
