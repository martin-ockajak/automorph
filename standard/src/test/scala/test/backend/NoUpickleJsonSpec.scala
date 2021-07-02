//package test.backend
//
//import jsonrpc.{Client, Handler}
//import jsonrpc.backend.IdentityBackend
//import jsonrpc.backend.IdentityBackend.Identity
//import jsonrpc.spi.Backend
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import test.codec.json.UpickleJsonSpec
//
//class IdentityUpickleJsonSpec extends UpickleJsonSpec {
//
//  type Effect[T] = Identity[T]
//  type Context = Short
//
//  override def run[T](effect: Effect[T]): T = effect
//
//  override lazy val backend: Backend[Effect] = IdentityBackend()
//
//  override lazy val client: Client[Node, ExactCodec, Effect, Context] =
//    Client(codec, backend, handlerTransport)
//
//  override lazy val handler: Handler[Node, ExactCodec, Effect, Context] =
//    Handler[Node, ExactCodec, Effect, Context](codec, backend)
//
//  lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
//}
