package test.backend.local

import jsonrpc.backend.ScalazBackend
import jsonrpc.spi.Backend
import jsonrpc.{Client, Handler}
import org.scalacheck.Arbitrary
import scala.util.Try
import scalaz.effect.IO
import test.CodecClientHandlerSpec

class ScalazLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = IO[T]
  type Context = Short

  override def run[T](effect: Effect[T]): T = effect.unsafePerformIO()

  override lazy val backend: Backend[Effect] = ScalazBackend()

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
}
