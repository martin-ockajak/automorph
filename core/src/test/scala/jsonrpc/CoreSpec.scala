package jsonrpc

import base.BaseSpec
import jsonrpc.client.Client
import jsonrpc.handler.Handler
import jsonrpc.spi.{Backend, Codec, Transport}
import jsonrpc.{ComplexApi, ComplexApiImpl, SimpleApi}

trait CoreSpec[Node, CodecType <: Codec[Node], Effect[_]] extends BaseSpec:

  val simpleApi = SimpleApi(backend)
  val complexApi = ComplexApiImpl(backend)

  def codec: CodecType

  def backend: Backend[Effect]

  inline def transport: Transport[Effect, Short]

  inline def client: Client[Node, CodecType, Effect, Short]

  inline def handler: Handler[Node, CodecType, Effect, Short]

//  def handlerx: Handler[Node, CodecType, Effect, Short]

  inline def simpleApiProxy: SimpleApi[Effect]

  inline def complexApiProxy: ComplexApi[Effect]

  "" - {
    "Bind" in {
      backend.pure(())
    }
  }
