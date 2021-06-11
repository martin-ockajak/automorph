//package jsonrpc.backend.standard
//
//import jsonrpc.backend.standard.FutureBackend
//import jsonrpc.codec.json.UpickleJsonSpec
//import jsonrpc.spi.Backend
//import jsonrpc.transport.local.HandlerTransport
//import jsonrpc.{Client, ComplexApi, InvalidApi, SimpleApi}
//import scala.concurrent.ExecutionContext.Implicits.global
//import jsonrpc.backend.standard.NoBackend.Identity
//
//class NoUpickleJsonSpec extends UpickleJsonSpec:
//  type Effect[T] = Identity[T]
//
//  override def backend: Backend[Effect] = NoBackend()
//
//  override def run[T](effect: Effect[T]): T = effect
//
//  override def client: Client[Node, CodecType, Effect, Short] =
//    Client(codec, backend, handlerTransport)
//
//  override def simpleApis: Seq[SimpleApi[Effect]] = clients.map(_.bind[SimpleApi[Effect]])
//
//  override def complexApis: Seq[ComplexApi[Effect]] = clients.map(_.bind[ComplexApi[Effect]])
//
//  override def invalidApis: Seq[InvalidApi[Effect]] = clients.map(_.bind[InvalidApi[Effect]])
