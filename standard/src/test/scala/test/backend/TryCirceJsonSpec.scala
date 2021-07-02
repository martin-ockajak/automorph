package test.backend.standard

import jsonrpc.{Client, Handler}
import jsonrpc.backend.TryBackend
import jsonrpc.spi.Backend
import org.scalacheck.Arbitrary
import scala.util.Try
import test.codec.json.CirceJsonSpec

class TryCirceJsonSpec extends CirceJsonSpec {

  type Effect[T] = Try[T]
  type Context = Short

  override def backend: Backend[Effect] = TryBackend()

  override def run[T](effect: Effect[T]): T = effect.get

  override lazy val client: Client[Node, CodecType, Effect, Context] =
    Client(codec, backend, handlerTransport)

  override lazy val handler: Handler[Node, CodecType, Effect, Context] =
    Handler[Node, CodecType, Effect, Context](codec, backend)

  lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
}
