package test.backend

import jsonrpc.backend.TryBackend
import jsonrpc.spi.Backend
import jsonrpc.{Client, Handler}
import org.scalacheck.Arbitrary
import scala.util.Try
import test.codec.json.UpickleJsonSpec

class TryUpickleJsonSpec extends UpickleJsonSpec {

  type Effect[T] = Try[T]
  type Context = Short

  override def backend: Backend[Effect] = TryBackend()

  override def run[T](effect: Effect[T]): T = effect.get

  override lazy val client: Client[Node, ExactCodec, Effect, Context] =
    Client(codec, backend, handlerTransport)

  override lazy val handler: Handler[Node, ExactCodec, Effect, Context] =
    Handler[Node, ExactCodec, Effect, Context](codec, backend)

  lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])
}
