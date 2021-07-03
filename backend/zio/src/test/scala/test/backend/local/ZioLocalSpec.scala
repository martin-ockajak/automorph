package test.backend.local

import jsonrpc.backend.ZioBackend
import jsonrpc.spi.Backend
import jsonrpc.{Client, Handler}
import org.scalacheck.Arbitrary
import test.CodecClientHandlerSpec
import zio.{RIO, Runtime, ZEnv}

class ZioLocalSpec extends CodecClientHandlerSpec {

  type Effect[T] = RIO[ZEnv, T]
  type Context = Short

  override def run[T](effect: Effect[T]): T = Runtime.default.unsafeRunTask(effect)

  override lazy val backend: Backend[Effect] = ZioBackend[ZEnv]()

  override lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
}
