package test.backend.local

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import jsonrpc.backend.CatsBackend
import jsonrpc.spi.Backend
import org.scalacheck.Arbitrary
import test.CodecClientHandlerSpec

class CatsLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = IO[T]
  type Context = Short

  override def run[T](effect: Effect[T]): T = effect.unsafeRunSync()

  override lazy val backend: Backend[Effect] = CatsBackend()

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
}
