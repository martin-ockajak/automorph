package test.backend.local

import jsonrpc.backend.CatsBackend
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import jsonrpc.spi.Backend
import jsonrpc.{Client, Handler}
import org.scalacheck.Arbitrary
import scala.util.Try
import test.CodecClientHandlerSpec

class CatsLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = IO[T]
  type Context = Short

  override def run[T](effect: Effect[T]): T = Try(effect.unsafeRunSync()).get

  override lazy val backend: Backend[Effect] = CatsBackend()

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
}
