//package test.backend
//
//import jsonrpc.{Client, Handler}
//import jsonrpc.backend.NoBackend
//import jsonrpc.backend.NoBackend.Identity
//import jsonrpc.spi.Backend
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import test.codec.json.UpickleJsonSpec
//
//class NoUpickleJsonSpec extends UpickleJsonSpec {
//
//  type Effect[T] = Identity[T]
//  type Context = Short
//
//  override def run[T](effect: Effect[T]): T = effect
//
//  override lazy val backend: Backend[Effect] = NoBackend()
//
//  override lazy val client: Client[Node, CodecType, Effect, Context] =
//    Client(codec, backend, handlerTransport)
//
//  override lazy val handler: Handler[Node, CodecType, Effect, Context] =
//    Handler[Node, CodecType, Effect, Context](codec, backend)
//
//  lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
//}
