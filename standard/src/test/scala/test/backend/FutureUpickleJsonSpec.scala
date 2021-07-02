package test.backend

import jsonrpc.{Client, Handler}
import jsonrpc.backend.FutureBackend
import jsonrpc.spi.Backend
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.codec.json.UpickleJsonSpec

class FutureUpickleJsonSpec extends UpickleJsonSpec {

  type Effect[T] = Future[T]
  type Context = Short

  override def run[T](effect: Effect[T]): T = await(effect)

  override lazy val backend: Backend[Effect] = FutureBackend()

  override lazy val client: Client[Node, CodecType, Effect, Context] =
    Client(codec, backend, handlerTransport)

  override lazy val handler: Handler[Node, CodecType, Effect, Context] =
    Handler[Node, CodecType, Future, Context](codec, backend)

  lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  "" - {
    "test" in {
      implicit val context: Context = arbitraryContext.arbitrary.sample.get
      client.callByPosition[String, String]("test", "test")
    }
  }
}
