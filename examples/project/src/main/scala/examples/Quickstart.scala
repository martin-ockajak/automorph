package examples

import automorph.Default
import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

private[examples] object Quickstart extends App {
  val currentThread = Thread.currentThread
  val latch = new java.util.concurrent.CountDownLatch(1)
  val diagnostic = new Runnable {

    override def run(): Unit = {
      println("THREAD")
//      latch.countDown()
      System.out.flush()
      Thread.sleep(1)
      println("STACKTRACE")
//      println(currentThread.getState)
      currentThread.getStackTrace.foreach(println)
      println()
      System.out.flush()
    }
  }
  new Thread(diagnostic).start()

  // Create server API instance
//  class ServerApi {
//
////    def hello(some: String, n: Int): Future[String] = {
//    def hello(some: String): Future[String] = {
//      println("API FUNCTION CALLED")
//      System.out.flush()
////      Future(s"Hello $some $n!")
//      Future(s"Hello $some!")
//    }
//  }
//  val api = new ServerApi()
//
//  // Start JSON-RPC HTTP server listening on port 7000 for requests to '/api'
//  val serverBuilder = Default.serverBuilderAsync(7000, "/api")
//  val server = serverBuilder(_.bind(api))

  // Define client view of the remote API
//  trait ClientApi {
////    def hello(some: String, n: Int): Future[String]
//    def hello(some: String): Future[String]
//  }
//
//  // Setup JSON-RPC HTTP client sending POST requests to 'http://localhost:7000/api'
//  println("STUFF")
//  val client = Default.clientAsync(new URI("http://localhost:7000/api"))
//
//  // Call the remote API function statically
//  val remoteApi = client.bind[ClientApi]

//  Thread.sleep(1000)
  println("WAIT")
//  latch.await()

//  Thread.sleep(5000)
  Range(0, Int.MaxValue).foreach { _ =>
    Range(0, Int.MaxValue).foreach { _ =>
      ()
    }
  }
  println("DONE")
//  println(Await.result(
////    remoteApi.hello("world", 1),
//    remoteApi.hello("world"),
//    Duration.Inf
//  ))

  // Call the remote API function dynamically
//  println(Await.result(
//    client.call[String]("hello").args("some" -> "world", "n" -> 1),
//    Duration.Inf
//  ))
//
//  // Close the client
//  Await.result(client.close(), Duration.Inf)
//
//  // Stop the server
//  Await.result(server.close(), Duration.Inf)
}
