//package jsonrpc
//
//import base.BaseSpec
//import jsonrpc.client.ClientFactory
//import jsonrpc.handler.{Handler, HandlerFactory}
//import jsonrpc.spi.{Backend, Codec}
//import jsonrpc.transport.local.HandlerTransport
//import jsonrpc.{ComplexApiImpl, Enum, Record, SimpleApi, Structure}
//
//trait CoreSpec[Node, CodecType <: Codec[Node], Effect[_]] extends BaseSpec:
//
//  inline def codec: CodecType
//
//  inline def backend: Backend[Effect]
//
//  "" - {
//    val simpleApi = SimpleApi(backend)
//    val complexApi = ComplexApiImpl(backend)
//    val handler = createHandler(simpleApi, complexApi)
//    val transport = HandlerTransport(handler, backend)
//    val client = ClientFactory[Node, CodecType, Effect, Short](codec, backend, transport)
//    val simpleApiProxy = client.bind[SimpleApi[Effect]]
//    val complexApiProxy = client.bind[ComplexApi[Effect]]
//    "Bind" in {
//    }
//  }
//
//  inline def createHandler(simpleApi: SimpleApi[Effect], complexApi: ComplexApi[Effect]): Handler[Node, CodecType, Effect, Short] =
//    HandlerFactory[Node, CodecType, Effect, Short](codec, backend)
//      .bind(simpleApi)
//      .bind[ComplexApi[Effect]](complexApi)
