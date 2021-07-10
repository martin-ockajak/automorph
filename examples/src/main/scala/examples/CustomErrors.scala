//package examples
//
//object CustomErrors extends App {
//
//  // Define an API type and create API instance
//  class Api {
//    def regular(foo: Int)(bar: )
//
//    def aliased(test: Option[String]): Unit
//
//    def omitted(): String
//  }
//  val api = new Api()
//
//  // Create and start JSON-RPC server listening on port 80 for HTTP requests with URL path '/api'
//  val server = automorph.DefaultHttpServer.sync(_.bind(api), 80, "/api")
//
//  // Create JSON-RPC client for sending HTTP POST requests to 'http://localhost/api'
//  val client = automorph.DefaultHttpClient.sync("http://localhost/api", "POST")
//
//  // Call the remote API method via proxy
//  val apiProxy = client.bind[Api] // Api
//  apiProxy.hello("world", 1) // : String
//
//  // Stop the server
//  server.close()
//}
