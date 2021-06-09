package jsonrpc

import base.BaseSpec
import jsonrpc.client.UnnamedBinding
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.{ComplexApi, ComplexApiImpl, SimpleApi, SimpleApiImpl}
import scala.concurrent.Future
import scala.util.Try

trait CoreSpec[Node, CodecType <: Codec[Node], Effect[_]] extends BaseSpec:

  val simpleApiInstance = SimpleApiImpl(backend)
  val complexApiInstance = ComplexApiImpl(backend)

  def backend: Backend[Effect]

  def run[T](effect: Effect[T]): T

  def client: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]]

  def simpleApiLocal: SimpleApi[Effect]

  def complexApiLocal: ComplexApi[Effect]

  def simpleApiRemote: SimpleApi[Effect]

  def complexApiRemote: ComplexApi[Effect]

  case class Arguments(
    x: String,
    y: Int
  )

  private def apiCombinations[Api](originalApi: Api, boundApis: (String, Api)*): Seq[(String, Seq[Api])] =
    boundApis.map((binding, api) => binding -> Seq(originalApi, api))

  "" - {
    "SimpleApi" - {
      apiCombinations("Instance" -> simpleApiInstance, "Local" -> simpleApiLocal, "Remote" -> simpleApiRemote).foreach { case (bindings, apis) =>
        bindings - {
          "test" in {
//             val Seq(result1, result2) = apis.map(api => run(api.test("test")))
//             result1.should(equal(result2))
          }
        }
      }
    }
    "ComplexApi" - {
      apiCombinations("Instance" -> complexApiInstance, "Local" -> complexApiLocal, "Remote" -> complexApiRemote).foreach { case (binding, apis) =>
        binding - {
          "method0" in {
//            val Seq(result1, result2) = apis.map(api => run(api.method0()))
//             results1.should(equal(result2))
          }
        }
      }
    }
    "Dynamic" in {
//      client.callByName[Int]("test")("a", "b")(1, 2, 3)(using 0)
//      client.bind("test").parameters("a", "b").call[Int](1, 2, 3)(using 0)
//      client.bind("test").call[Arguments, Int](Arguments("test", 1))(using 0)
//      val x = client.callByName[Int]("test")("a", "b")
//      val y = x(1, 2, 3)(using 0)
//      y(0)
    }
  }
