package test

import base.BaseSpec
import jsonrpc.Client
import jsonrpc.protocol.ErrorType.MethodNotFoundException
import jsonrpc.spi.{Backend, Codec}
import org.scalacheck.Arbitrary
import scala.util.Try
import test.Generators.arbitraryRecord
import test.{ComplexApi, ComplexApiImpl, InvalidApi, InvalidApiImpl, SimpleApi, SimpleApiImpl}

/**
 * Main client -> handler API method invocation test.
 *
 * Checks the results of remote method invocations against identical local invocations.
 *
 * @tparam Node message format node representation type
 * @tparam CodecType message codec plugin type
 * @tparam Effect effect type
 */
trait ClientHandlerSpec extends BaseSpec {

  type Node
  type CodecType <: Codec[Node]
  type Effect[_]
  type Context

  val simpleApiInstance: SimpleApi[Effect] = SimpleApiImpl(backend)
  val complexApiInstance: ComplexApi[Effect, Context] = ComplexApiImpl(backend)
  val invalidApiInstance: InvalidApi[Effect] = InvalidApiImpl(backend)
  private val apiNames = Seq("Named", "Positional")

  def backend: Backend[Effect]

  def run[T](effect: Effect[T]): T

  def client: Client[Node, CodecType, Effect, Context]

  def simpleApis: Seq[SimpleApi[Effect]]

  def complexApis: Seq[ComplexApi[Effect, Context]]

  def invalidApis: Seq[InvalidApi[Effect]]

  def clients: Seq[Client[Node, CodecType, Effect, Context]] = Seq(
    client,
    client.copy(argumentsByName = false)
  )

  implicit def arbitraryContext: Arbitrary[Context]

  "" - {
    "Trait" - {
      "Call" - {
        "Simple API" - {
          apiCombinations(simpleApiInstance, simpleApis, apiNames).foreach { case (mode, apis) =>
            mode - {
              "test" in {
                check { (a0: String) =>
                  consistent(apis, (api: SimpleApi[Effect]) => api.test(a0))
                }
              }
            }
          }
        }
        "Complex API" - {
          apiCombinations(complexApiInstance, complexApis, apiNames).foreach { case (mode, apis) =>
            mode - {
              "method0" in {
                check((_: Unit) => {
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method0())
                })
              }
              "method1" in {
                check { (_: Unit) =>
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method1())
                }
              }
              "method2" in {
                check{ (a0: String) =>
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method2(a0))
                }
              }
              "method3" in {
                check { (a0: Float, a1: Long, a2: Option[Seq[Int]]) =>
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method3(a0, a1, a2))
                }
              }
              "method4" in {
                check { (a0: BigDecimal, a1: Byte, a2: Map[String, Int], a3: Option[String]) =>
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method4(a0, a1, a2, a3))
                }
              }
              "method5" in {
                check { (a0: Boolean, a1: Short, a2: List[Int]) =>
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method5(a0, a1)(a2))
                }
              }
              "method6" in {
                check { (a0: Record, a1: Double) =>
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method6(a0, a1))
                }
              }
              "method7" in {
                check { (a0: Record, a1: Boolean, context: Context) =>
                  implicit val usingContext: Context = context
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method7(a0, a1))
                }
              }
              "method8" in {
                check { (a0: Record, a1: String, a2: Option[Double], context: Context) =>
                  implicit val usingContext: Context = context
                  consistent(apis, (api: ComplexApi[Effect, Context]) => api.method8(a0, a1, a2))
                }
              }
              "method9" in {
                check { (a0: String) =>
                  val (referenceApi, testedApi) = apis
                  val expected = Try(run(referenceApi.method9(a0))).toEither
                  val result = Try(run(testedApi.method9(a0))).toEither
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
              val (_, api) = apis
              "Method not found" in {
                val error = intercept[MethodNotFoundException](run(api.nomethod(""))).getMessage.toLowerCase
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
                  run(api.method3(0, None))
                }.getMessage.toLowerCase
                error.should(include("expected"))
                error.should(include("null"))
              }
              "Optional arguments" in {
                run(api.method3(0, Some(0)))
              }
              "Invalid argument" in {
                val error = intercept[RuntimeException] {
                  run(api.method4(0, 0, ""))
                }.getMessage.toLowerCase
                error.should(include("expected"))
                error.should(include("string"))
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
              check { (a0: String, context: Context) =>
                implicit val usingContext: Context = context
//                run(client.callByPosition[String, String]("test", a0))
                true
              }
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

  private def apiCombinations[Api](originalApi: Api, apis: Seq[Api], names: Seq[String]): Seq[(String, (Api, Api))] =
    apis.zip(names).map { case (api, name) =>
      name -> ((originalApi, api))
    }

  private def consistent[Api, Result](apis: (Api, Api), function: Api => Effect[Result]): Boolean = {
    val (referenceApi, testedApi) = apis
    val expected = run(function(referenceApi))
    val result = run(function(testedApi))
    expected.equals(result)
  }
}
