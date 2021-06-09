package jsonrpc.core

import base.BaseSpec
import jsonrpc.client.UnnamedBinding
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.{Client, ComplexApi, ComplexApiImpl, SimpleApi, SimpleApiImpl}
import scala.concurrent.Future
import scala.util.Try

trait ClientHandlerSpec[Node, CodecType <: Codec[Node], Effect[_]] extends BaseSpec:
  case class TestedApis[Api](
    namedLocal: Api,
    positionalLocal: Api,
    namedRemote: Api,
    positionalRemote: Api
  )

  val simpleApiInstance = SimpleApiImpl(backend)
  val complexApiInstance = ComplexApiImpl(backend)
  private val testApiNamePattern = """^(\w+)([A-Z]\w+)$""".r

  def backend: Backend[Effect]

  def run[T](effect: Effect[T]): T

  def localClient: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]]

  def simpleApis: TestedApis[SimpleApi[Effect]]

  def complexApis: TestedApis[ComplexApi[Effect]]

  case class Arguments(
    x: String,
    y: Int
  )

  "" - {
    "Call" - {
      "Trait" - {
        "Simple API" - {
          apiCombinations(simpleApiInstance, simpleApis).foreach { case (bindings, apis) =>
            bindings - {
              "test" ignore {
                val Seq(expected, result) = apis.map(api => run(api.test("test")))
                expected.should(equal(result))
              }
            }
          }
        }
        "Complex API" - {
          apiCombinations(complexApiInstance, complexApis).foreach { case (binding, apis) =>
            binding - {
              "method0" ignore {
                val Seq(expected, result) = apis.map(api => run(api.method0()))
                expected.should(equal(result))
              }
            }
          }
        }
      }
      "Tuple" - {
        "Named / Local" - {
          "Simple API" in {
//            client.callByName[Int]("test")("a", "b")(1, 2, 3)(using 0)
//            client.bind("test").parameters("a", "b").call[Int](1, 2, 3)(using 0)
//            val x = client.callByName[Int]("test")("a", "b")
//            val y = x(1, 2, 3)(using 0)
//            y(0)
            }
        }
        "Positional" - {
          "Simple API" in {
//            client.callByPosition[Int]("test")(1, 2, 3)(using 0)
          }
        }
      }
      "Case class" - {
        "Named / Local" - {
          "Simple API" in {
//            client.bind("test").call[Arguments, Int](Arguments("test", 1))(using 0)
          }
        }
      }
    }
    "Notify" - {
      "Tuple" - {
        "Named / Local" ignore {

        }
        "Positional / Local" ignore {

        }
      }
      "Case class" - {
        "Named" ignore {

        }
      }
    }
  }

  private def apiCombinations[Api](originalApi: Api, testedApis: TestedApis[Api]): Seq[(String, Seq[Api])] =
    testedApis.productElementNames.zipWithIndex.map { case (binding, index) =>
      val (outer, inner) = binding match
       case testApiNamePattern(first, second) => first -> second
       case name => name -> "[None]"
      s"${outer.capitalize} / $inner" ->Seq(originalApi, testedApis.productElement(index).asInstanceOf[Api])
    }.toSeq
