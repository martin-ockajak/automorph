//package test.http
//
//import automorph.system.FutureBackend
//import automorph.server.http.NanoHttpdServer
//import automorph.spi.Backend
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//import test.CodecClientHandlerSpec
//
//class FutureHttpSpec extends CodecClientHandlerSpec {
//
//  type Effect[T] = Future[T]
//  type Context = Short
//
//  private lazy val server = {
//    val httpPort = availablePort
//    NanoHttpdServer()
//  }
//
//  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
//
//  override lazy val backend: Backend[Effect] = FutureBackend()
//
//  override def run[T](effect: Effect[T]): T = await(effect)
//
//  override def afterAll(): Unit = {
//    server.close()
//    super.afterAll()
//  }
//}
