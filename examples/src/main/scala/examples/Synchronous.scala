package examples

object Synchronous extends App {

  class SyncApi {
    def hello(some: String, n: Int): String = s"Hello $some $n!"
  }

  val syncApi = new SyncApi() // SyncApi

  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
  val syncServer = automorph.DefaultHttpServer.sync(_.bind(syncApi), 80, "/api")

  // Create JSON-RPC client sending HTTP POST requests to 'http://localhost/api'
  val syncClient = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")

  // Call the remote API method via proxy
  val syncApiProxy = syncClient.bind[SyncApi]
  syncApiProxy.hello("world", 1) // : String

  // Stop the server
  syncServer.close()
}
