package test.local

import jsonrpc.backend.FutureBackend
import jsonrpc.spi.Backend
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.CodecClientHandlerSpec

class FutureLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = Future[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: Backend[Effect] = FutureBackend()

  override def run[T](effect: Effect[T]): T = await(effect)
}
