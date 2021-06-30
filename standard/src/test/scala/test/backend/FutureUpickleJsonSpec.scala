package test.backend

import jsonrpc.{Client, Handler}
import jsonrpc.backend.FutureBackend
import jsonrpc.spi.Backend
import org.scalacheck.Arbitrary
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import test.codec.json.UpickleJsonSpec
import test.{ComplexApi, InvalidApi, SimpleApi}

class FutureUpickleJsonSpec extends UpickleJsonSpec {

  type Effect[T] = Future[T]
  type Context = Short

  override def run[T](effect: Effect[T]): T = await(effect)

  override lazy val backend: Backend[Effect] = FutureBackend()

  override lazy val client: Client[Node, CodecType, Effect, Context] =
    Client(codec, backend, handlerTransport)

  override lazy val handler: Handler[Node, CodecType, Effect, Context] =
    Handler[Node, CodecType, Future, Context](codec, backend)
      .bind(simpleApiInstance).bind[ComplexApi[Future, Context]](complexApiInstance)

  override def simpleApis: Seq[SimpleApi[Future]] = clients.map(_.bind[SimpleApi[Future]])

  override def complexApis: Seq[ComplexApi[Future, Context]] = clients.map(_.bind[ComplexApi[Future, Context]])

  override def invalidApis: Seq[InvalidApi[Future]] = clients.map(_.bind[InvalidApi[Future]])

  lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  "" - {
    "test" in {
      implicit val context: Context = arbitraryContext.arbitrary.sample.get

      client.callByPosition[String, String]("test", "test")
    }
  }
}
