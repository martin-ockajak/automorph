package jsonrpc

import base.BaseSpec
import jsonrpc.client.Client
import jsonrpc.handler.Handler
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.{ComplexApi, ComplexApiImpl, SimpleApi}

trait CoreSpec[Node, CodecType <: Codec[Node], Effect[_]] extends BaseSpec:

  val simpleApi = SimpleApi(backend)
  val complexApi = ComplexApiImpl(backend)

  def backend: Backend[Effect]

  def client: Client[Node, CodecType, Effect, Short]

  def simpleApiProxy: SimpleApi[Effect]

  def complexApiProxy: ComplexApi[Effect]

  "" - {
    "Bind" in {
      client.backend
    }
  }
