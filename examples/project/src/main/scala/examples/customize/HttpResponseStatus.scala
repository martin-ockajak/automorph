//package examples.customize
//
//import automorph.Default
//import automorph.transport.http.HttpContext
//import java.net.URI
//import java.sql.SQLException
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.duration.Duration
//import scala.concurrent.{Await, Future}
//import scala.util.Try
//
//private[examples] object HttpResponseStatus {
//  @scala.annotation.nowarn
//  def main(arguments: Array[String]): Unit = {
//
//    // Create server API instance
//    class ServerApi {
//      def hello(some: String, n: Int): Future[String] =
//        Future.failed(new SQLException("Test error"))
//    }
//    val api = new ServerApi()
//
//    // Customize remote API server exception to HTTP status code mapping
//    val serverBuilder = Default.serverBuilderAsync(7000, "/api", mapException = {
//      case _: SQLException => 400
//      case e => HttpContext.defaultExceptionToStatusCode(e)
//    })
//
//    // Start custom JSON-RPC HTTP server listening on port 7000 for requests to '/api'
//    val server = serverBuilder(_.bind(api))
//
//    // Define client view of the remote API
//    trait ClientApi {
//      def hello(some: String, n: Int): Future[String]
//    }
//    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
//    val client = Default.clientAsync(new URI("http://localhost:7000/api"))
//
//    // Call the remote API function and fail with InvalidRequestException
//    val remoteApi = client.bind[ClientApi]
//    println(Try(Await.result(
//      remoteApi.hello("world", 1),
//      Duration.Inf
//    )).failed.get)
//
//    // Close the client
//    Await.result(client.close(), Duration.Inf)
//
//    // Stop the server
//    Await.result(server.close(), Duration.Inf)
//  }
//}
