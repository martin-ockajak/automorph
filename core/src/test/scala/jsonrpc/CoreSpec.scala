package jsonrpc

import base.BaseSpec
import jsonrpc.client.UnnamedBinding
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.{ComplexApi, ComplexApiImpl, SimpleApi, SimpleApiImpl}

trait CoreSpec[Node, CodecType <: Codec[Node], Effect[_]] extends BaseSpec:

  val simpleApi = SimpleApiImpl(backend)
  val complexApi = ComplexApiImpl(backend)

  def backend: Backend[Effect]

  def client: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]]

  def simpleApiProxy: SimpleApi[Effect]

  def complexApiProxy: ComplexApi[Effect]

  case class Arguments(
    x: String,
    y: Int
  )

  "" - {
    "Bind" in {
      client.backend
//      val x = client.bind("test").parameters("a", "b").call[Int](1, 2, 3)(using 0)
//      val x = client.bind("test").call[Arguments, Int](Arguments("test", 1))(using 0)
//      val y = x(1, 2, 3)(using 0)
//      y(0)
    }
  }
