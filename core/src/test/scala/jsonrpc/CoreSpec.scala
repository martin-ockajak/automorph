package jsonrpc

import base.BaseSpec
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.{ComplexApi, ComplexApiImpl, SimpleApi, SimpleApiImpl}

trait CoreSpec[Node, CodecType <: Codec[Node], Effect[_]] extends BaseSpec:

  val simpleApi = SimpleApiImpl(backend)
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
