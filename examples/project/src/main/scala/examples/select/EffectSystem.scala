//package examples.select
//
//import automorph.Default
//import automorph.system.ZioSystem
//import java.net.URI
//import zio.{Task, Unsafe, ZIO}
//
//private[examples] object EffectSystem {
//  @scala.annotation.nowarn
//  def main(arguments: Array[String]): Unit = {
//
//    // Create server API instance
//    class ServerApi {
//      def hello(some: String, n: Int): Task[String] =
//        ZIO.succeed(s"Hello $some $n!")
//    }
//    val api = new ServerApi()
//
//    // Create ZIO effect system plugin
//    val system = ZioSystem.default
//
//    // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
//    val serverBuilder = Default.serverBuilder(system, 7000, "/api")
//    val server = serverBuilder(_.bind(api))
//
//    // Define client view of the remote API
//    trait ClientApi {
//      def hello(some: String, n: Int): Task[String]
//    }
//
//    // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
//    val client = Default.client(system, new URI("http://localhost:7000/api"))
//
//    // Define a helper function to run ZIO tasks
//    def run[T](effect: Task[T]): T =
//      Unsafe.unsafe { implicit unsafe =>
//        ZioSystem.defaultRuntime.unsafe.run(effect).toEither.swap.map(_.getCause).swap.toTry.get
//      }
//
//    // Call the remote APi function via proxy
//    val remoteApi = client.bind[ClientApi]
//    println(run(
//      remoteApi.hello("world", 1)
//    ))
//
//    // Close the client
//    run(client.close())
//
//    // Stop the server
//    run(server.close())
//  }
//}
