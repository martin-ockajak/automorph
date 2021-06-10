package jsonrpc.core

import base.BaseSpec
import jsonrpc.client.UnnamedBinding
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.{Client, ComplexApi, ComplexApiImpl, SimpleApi, SimpleApiImpl}
import scala.concurrent.Future
import scala.util.Try
import org.scalacheck.Prop
import org.scalacheck.Gen

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
  private val integers = Gen.choose(-2000000000, 2000000000)

  def backend: Backend[Effect]

  def run[T](effect: Effect[T]): T

  def localClient: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]]

  def remoteClient: Client[Node, CodecType, Effect, Short, UnnamedBinding[Node, CodecType, Effect, Short]]

  def simpleApis: TestedApis[SimpleApi[Effect]]

  def complexApis: TestedApis[ComplexApi[Effect]]

  case class Arguments(
    x: String,
    y: Int
  )

  "" - {
    "Trait" - {
      "Call" - {
        "Simple API" - {
          apiCombinations(simpleApiInstance, simpleApis).foreach { case (outerTest, tests) =>
            outerTest - {
              tests.foreach { case (innerTest, apis) =>
                innerTest - {
                  "test" in {
                    check(Prop.forAll { (a0: String) =>
                      consistent(apis, _.test(a0))
                    })
                  }
                }
              }
            }
          }
        }
        "Complex API" - {
          apiCombinations(complexApiInstance, complexApis).foreach { case (outerTest, tests) =>
            outerTest - {
              tests.foreach { case (innerTest, apis) =>
                innerTest - {
                  "method0" in {
                    check(Prop.forAll { () =>
                      consistent(apis, _.method0())
                    })
                  }
                  "method1" in {
                    check(Prop.forAll { () =>
                      consistent(apis, _.method1())
                    })
                  }
                  "method2" in {
                    check(Prop.forAll { (a0: String) =>
                      consistent(apis, _.method2(a0))
                    })
                  }
                  "method3" in {
                    check(Prop.forAll(integers) { (a0: Int) =>
                      consistent(apis, _.method3(0))
                    })
                  }
                  "method4" in {
                    check(Prop.forAll { (a0: Option[Int]) =>
                      consistent(apis, _.method4(a0))
                    })
                  }
                  "method5" in {
                    check(Prop.forAll { (a0: String, a1: Int) =>
                      consistent(apis, _.method5(a0, a1))
                    })
                  }
                }
              }
            }
          }
        }
      }
    }
    "Tuple" - {
      "Call" - {
        "Simple API" - {
          "Named" - {
            "Local" in {
//              localClient.callByName[Int]("test")("a", "b")(1, 2, 3)(using 0)
//              localClient.bind("test").parameters("a", "b").call[Int](1, 2, 3)(using 0)
//              val x = localClient.callByName[Int]("test")("a", "b")
//              val y = x(1, 2, 3)(using 0)
//              y(0)
            }
          }
          "Positional" - {
            "Local" in {
//              localClient.callByPosition[Int]("test")(1, 2, 3)(using 0)
            }
          }
        }
      }
      "Notify" - {
        "Simple API" - {
          "Named" - {
            "Local" ignore {}
          }
          "Positional" - {
            "Local" ignore {}
          }
        }
      }
    }
    "Case class" - {
      "Call" - {
        "Simple API" - {
          "Named" - {
            "Local" in {
//              localClient.bind("test").call[Arguments, Int](Arguments("test", 1))(using 0)
            }
          }
        }
      }
      "Notify" - {
        "Simple API" - {
          "Named" - {
            "Local" ignore {}
          }
        }
      }
    }
  }

  private def apiCombinations[Api](originalApi: Api, testedApis: TestedApis[Api]): Seq[(String, Seq[(String, Seq[Api])])] =
    val tests = testedApis.productElementNames.zipWithIndex.map { case (name, index) =>
      val (outer, inner) =
        name match
          case testApiNamePattern(first, second) => first -> second
          case first                              => first -> "[None]"
      (outer.capitalize, inner, Seq(originalApi, testedApis.productElement(index).asInstanceOf[Api]))
    }.toSeq
    tests.groupBy(_._1).view.mapValues(_.map { case (_, inner, apis) =>
      inner -> apis
    }).toSeq

  private def consistent[Api, R](apis: Seq[Api], function: Api => Effect[R]): Boolean =
    val Seq(expected, result) = apis.map(api => run(function(api)))
    expected.equals(result)

  private def call[Api, R](apis: Seq[Api], function: Api => Effect[R]): Unit =
    val Seq(expected, result) = apis.map(api => run(function(api)))
    expected.should(equal(result))
