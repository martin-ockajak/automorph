//package test.backend.standard
//
//import jsonrpc.spi.Backend
//import jsonrpc.{Client, ComplexApi, InvalidApi, SimpleApi}
//import jsonrpc.backend.NoBackend
//import jsonrpc.backend.NoBackend.Identity
//import test.codec.json.UpickleJsonSpec
//
//class NoUpickleJsonSpec extends UpickleJsonSpec {
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
//}
