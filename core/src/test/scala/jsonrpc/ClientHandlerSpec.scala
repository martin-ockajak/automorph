package jsonrpc

import base.BaseSpec
import jsonrpc.protocol.Errors.MethodNotFound
import jsonrpc.spi.{Backend, Codec}
import jsonrpc.{Client, ComplexApi, ComplexApiImpl, InvalidApi, InvalidApiImpl, SimpleApi, SimpleApiImpl}
import jsonrpc.Generators.arbitraryRecord
import scala.util.Try

/**
 * Main client -> handler API method invocation test.
 *
 * Checks the results of remote method invocations against identical local invocations.
 *
 * @tparam Node message format node representation type
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 */
trait ClientHandlerSpec extends BaseSpec:

  type Node
  type CodecType <: Codec[Node]
  type Effect[_]

  val simpleApiInstance = SimpleApiImpl(backend)
  val complexApiInstance = ComplexApiImpl(backend)
  val invalidApiInstance = InvalidApiImpl(backend)
  private val apiNames = Seq("Named", "Positional")
  private val testApiNamePattern = """^(\w+)([A-Z]\w+)$""".r

  def backend: Backend[Effect]

  def run[T](effect: Effect[T]): T

  def client: Client[Node, CodecType, Effect, Short]

  def clients: Seq[Client[Node, CodecType, Effect, Short]] = Seq(
    client, client.copy(argumentsByName = false)
  )

  def simpleApis: Seq[SimpleApi[Effect]]

  def complexApis: Seq[ComplexApi[Effect]]

  def invalidApis: Seq[InvalidApi[Effect]]

  "" - {
    "Trait" - {
      "Call" - {
        "Simple API" - {
          apiCombinations(simpleApiInstance, simpleApis, apiNames).foreach { case (mode, apis) =>
            mode - {
              "test" in {
                check { (a0: String) =>
                  consistent(apis, _.test(a0))
                }
              }
            }
          }
        }
        "Complex API" - {
          apiCombinations(complexApiInstance, complexApis, apiNames).foreach { case (mode, apis) =>
            mode - {
              "method0" in {
                check { () =>
                  consistent(apis, _.method0())
                }
              }
              "method1" in {
                check { () =>
                  consistent(apis, _.method1())
                }
              }
              "method2" in {
                check { (a0: String) =>
                  consistent(apis, _.method2(a0))
                }
              }
              "method3" in {
                check { (a0: Short, a1: BigDecimal, a2: Seq[Int]) =>
                  consistent(apis, _.method3(a0, a1, a2))
                }
              }
              "method4" in {
                check { (a0: Long, a1: Byte, a2: Map[String, Int], a3: Option[String]) =>
                  consistent(apis, _.method4(a0, a1, a2, a3))
                }
              }
              "method5" in {
                check { (a0: Boolean, a1: Float, a2: List[Int]) =>
                  consistent(apis, _.method5(a0, a1)(a2))
                }
              }
              "method6" in {
                check { (a0: Record, a1: Double) =>
                  consistent(apis, _.method6(a0, a1))
                }
              }
              "method7" in {
                check { (a0: Record, a1: Boolean, context: Short) =>
                  consistent(apis, _.method7(a0, a1)(using context))
                }
              }
              "method8" in {
                check { (a0: Record, a1: String, a2: Option[Double], context: Short) =>
                  consistent(apis, _.method8(a0, a1, a2)(using context))
                }
              }
              "method9" in {
                check { (a0: String) =>
                  val Seq(expected, result) = apis.map(api => Try(run(api.method9(a0))).toEither)
                  val expectedErrorMessage = expected.swap.map(error =>
                    s"[${error.getClass.getSimpleName}] ${Option(error.getMessage).getOrElse("")}"
                  )
                  expectedErrorMessage.equals(result.swap.map(_.getMessage))
                }
              }
            }
          }
        }
        "Invalid API" - {
          apiCombinations(invalidApiInstance, invalidApis, apiNames).foreach { case (mode, apis) =>
            mode - {
              val api = apis.last
              "Method not found" in {
                val error = intercept[MethodNotFound](run(api.nomethod(""))).getMessage.toLowerCase
                error.should(include("nomethod"))
              }
              "Redundant arguments" in {
                val error = intercept[IllegalArgumentException](run(api.method1(""))).getMessage.toLowerCase
                error.should(include("redundant"))
              }
              "Invalid result" in {
                val error = intercept[IllegalStateException] {
                  run(api.method2(""))
                }.getMessage.toLowerCase
                error.should(include("invalid"))
              }
              "Missing arguments" in {
                val error = intercept[RuntimeException] {
                  run(api.method3(0))
                }.getMessage.toLowerCase
                error.should(include("null"))
              }
              "Invalid argument" in {
                val error = intercept[RuntimeException] {
                  run(api.method4(0, 0, ""))
                }.getMessage.toLowerCase
//                    error.should(include("expected"))
              }
            }
          }
        }
      }
    }
    "Direct" - {
      "Call" - {
        "Simple API" - {
          "Positional" - {
            "Local" in {
//              client.callByPosition[String, String]("test")("test")(using 0)
            }
          }
        }
      }
      "Notify" - {
        "Simple API" - {
          "Positional" - {
            "Local" ignore {}
          }
        }
      }
    }
  }

  private def apiCombinations[Api](originalApi: Api, apis: Seq[Api], names: Seq[String]): Seq[(String, Seq[Api])] =
    apis.zip(names).map { (api, name) =>
      name -> Seq(originalApi, api)
    }

  private def consistent[Api, Result](apis: Seq[Api], function: Api => Effect[Result]): Boolean =
    val Seq(expected, result) = apis.map(api => run(function(api)))
    expected.equals(result)
