package test.local

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import jsonrpc.backend.CatsEffectBackend
import jsonrpc.spi.Backend
import org.scalacheck.Arbitrary
import test.CodecClientHandlerSpec

class CatsEffectLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = IO[T]
  type Context = Short

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override lazy val backend: Backend[Effect] = CatsEffectBackend()

  override def run[T](effect: Effect[T]): T = effect.unsafeRunSync()
}
