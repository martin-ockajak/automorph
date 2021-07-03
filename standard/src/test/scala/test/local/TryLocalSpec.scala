package test.local

import jsonrpc.backend.TryBackend
import jsonrpc.spi.Backend
import jsonrpc.{Client, Handler}
import org.scalacheck.Arbitrary
import scala.util.Try
import test.CodecClientHandlerSpec

class TryLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = Try[T]
  type Context = Short

  override def backend: Backend[Effect] = TryBackend()

  override def run[T](effect: Effect[T]): T = effect.get

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
}
