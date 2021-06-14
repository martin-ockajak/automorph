package jsonrpc.backend.standard

import jsonrpc.codec.json.UpickleJsonSpec
import jsonrpc.spi.{Backend, Transport}
import jsonrpc.{Client, ComplexApi, Handler, InvalidApi, SimpleApi}
import scala.collection.immutable.ArraySeq
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FutureUpickleJsonSpec extends UpickleJsonSpec {
  type Effect[T] = Future[T]
  type Context = Short

  override def backend: Backend[Effect] = FutureBackend()

  override def run[T](effect: Effect[T]): T = await(effect)

  override def client: Client[Node, CodecType, Effect, Context] =
    Client(codec, backend, handlerTransport)

  lazy val arbitraryContext: Arbitrary[Context] = Arbitrary(Arbitrary.arbitrary[Context])

  override def simpleApis: Seq[SimpleApi[Effect]] = clients.map(_.bind[SimpleApi[Effect]])

  override def complexApis: Seq[ComplexApi[Effect, Context]] = clients.map(_.bind[ComplexApi[Effect, Context]])

  override def invalidApis: Seq[InvalidApi[Effect]] = clients.map(_.bind[InvalidApi[Effect]])

  "" - {
    "Bind" in {
      val errorTransport = new Transport[Effect, Client.NoContext] {
        def call(request: ArraySeq.ofByte, context: Option[Client.NoContext]): Effect[ArraySeq.ofByte] = ???

        def notify(request: ArraySeq.ofByte, context: Option[Client.NoContext]): Effect[Unit] = ???
      }
      val client = Client.basic(codec, backend, errorTransport)
      client.callByPosition[String, String]("test")("test")
    }
    "test" in {
      implicit val context: Context = arbitraryContext.arbitrary.sample.get

      client.callByPosition[String, String]("test")("test")
    }
  }
}
