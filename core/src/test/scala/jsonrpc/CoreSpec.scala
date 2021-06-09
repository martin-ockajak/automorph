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

  "" - {
    "SimpleApi" - {
      val apis = Seq("Instance" -> simpleApiInstance, "Local" -> simpleApiLocal, "Remote" -> simpleApiRemote)
      apis.indices.combinations(2).foreach {
        case Seq(index1, index2) =>
          val (binding1, api1) = apis(index1)
          val (binding2, api2) = apis(index2)
          s"$binding1 / $binding2" - {
            val results = apis.toMap.view.mapValues(api => () => run(api.test("test")))
            "test" in {
//            results(binding1)().should(equal(results(binding2)()))
            }
          }
          ()
        case _ => ()
      }
    }
    "ComplexApi" - {
      val apis = Seq("Instance" -> complexApiInstance, "Local" -> complexApiLocal, "Remote" -> complexApiRemote)
      apis.indices.combinations(2).foreach {
        case Seq(index1, index2) =>
          val (binding1, api1) = apis(index1)
          val (binding2, api2) = apis(index2)
          s"$binding1 / $binding2" - {
            val results = apis.toMap.view.mapValues(api => () => run(api.method0()))
            "test" in {
//            results(binding1)().should(equal(results(binding2)()))
            }
          }
          ()
        case _ => ()
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
