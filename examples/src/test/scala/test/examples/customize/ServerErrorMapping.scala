package test.examples.customize

import automorph.protocol.jsonrpc.ErrorType.InvalidRequest
import automorph.{Default, Handler}
import java.net.URI
import java.sql.SQLException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ServerErrorMapping extends App {

  // Create server API instance
  class ServerApi {
    def hello(some: String, n: Int): Future[String] =
      Future(s"Hello $some $n!")
  }
  val api = new ServerApi()

  // Customize remote API server exception to RPC error mapping
  val protocol = Default.protocol
  val serverProtocol = protocol.mapException {
    case _: SQLException => InvalidRequest
    case e => protocol.mapException(e)
  }

  // Start custom JSON-RPC HTTP server listening on port 8080 for requests to '/api'
  val system = Default.systemAsync
  val handler = Handler.protocol(serverProtocol).system(system)
    .context[Default.ServerContext]
  val server = Default.server(handler, 8080, "/api")

  // Define client view of a remote API
  trait ClientApi {
    def hello(some: String, n: Int): Future[String]
  }
  // Setup default JSON-RPC HTTP client sending POST requests to 'http://localhost/api'
  val client = Default.clientAsync(new URI("http://localhost/api"))

  // Call the remote API function
  val remoteApi = client.bind[ClientApi] // ClientApi
  remoteApi.hello("world", 1) // Future[String]

  // Close the client
  client.close()

  // Stop the server
  server.close()
}

class ServerErrorMapping extends org.scalatest.freespec.AnyFreeSpecLike {
  "" - {
    "Test" ignore {
      ServerErrorMapping.main(Array())
    }
  }
}
