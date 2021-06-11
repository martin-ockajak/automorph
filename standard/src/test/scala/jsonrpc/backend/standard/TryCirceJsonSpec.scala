//package jsonrpc.backend.standard
//
//import jsonrpc.backend.standard.TryBackend
//import jsonrpc.client.UnnamedBinding
//import jsonrpc.codec.json.UpickleJsonSpec
//import jsonrpc.spi.Backend
//import jsonrpc.transport.local.HandlerTransport
//import jsonrpc.{Client, ComplexApi, InvalidApi, SimpleApi}
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.util.Try
//
//class TryCirceJsonSpec extends UpickleJsonSpec:
//  type Effect[T] = Try[T]
//
//  override def backend: Backend[Effect] = TryBackend()
//
//  override def run[T](effect: Effect[T]): T = effect.get
//
//  override def client: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]] =
//    Client(codec, backend, handlerTransport)
//
//  override def simpleApis: Seq[SimpleApi[Effect]] = clients.map(_.bind[SimpleApi[Effect]])
//
//  override def complexApis: Seq[ComplexApi[Effect]] = clients.map(_.bind[ComplexApi[Effect]])
//
//  override def invalidApis: Seq[InvalidApi[Effect]] = clients.map(_.bind[InvalidApi[Effect]])
