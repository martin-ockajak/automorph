//package test.backend.local
//
//import jsonrpc.backend.IdentityBackend
//import jsonrpc.backend.IdentityBackend.Identity
//import jsonrpc.spi.Backend
//import jsonrpc.{Client, Handler}
//import org.scalacheck.Arbitrary
//import scala.concurrent.ExecutionContext.Implicits.global
//import test.CodecClientHandlerSpec
//
//class IdentityLocalSpec extends CodecClientHandlerSpec {
//
//  type Effect[T] = Identity[T]
//  type Context = Short
//
//  override def run[T](effect: Effect[T]): T = effect
//
//  override lazy val backend: Backend[Effect] = IdentityBackend()
//
//  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
//}
