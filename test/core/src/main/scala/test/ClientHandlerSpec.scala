package test

import base.BaseSpec
import automorph.{Client, Handler}
import automorph.protocol.ErrorType.{InvalidRequestException, InvalidResponseException, MethodNotFoundException}
import automorph.spi.Backend
import org.scalacheck.Arbitrary
import scala.util.Try
import test.Generators.arbitraryRecord
import test.{ComplexApi, ComplexApiImpl, InvalidApi, InvalidApiImpl, SimpleApi, SimpleApiImpl}

/**
 * Main client -> handler API method invocation test.
 *
 * Checks the results of remote method invocations against identical local invocations.
 *
 * @tparam Effect effect type
 * @tparam Context request context type
 */
trait ClientHandlerSpec extends BaseSpec {

  type Effect[_]
  type Context
  type SimpleApiType = SimpleApi[Effect]
  type ComplexApiType = ComplexApi[Effect, Context]
  type InvalidApiType = InvalidApi[Effect]

  case class CodecFixture(
    codec: Class[_],
    client: Client[_, _, Effect, Context],
    handler: Handler[_, _, Effect, Context],
    simpleApis: Seq[SimpleApiType],
    complexApis: Seq[ComplexApiType],
    invalidApis: Seq[InvalidApiType],
    positionalCall: (String, String) => Effect[String],
    namedCall: (String, (String, String)) => Effect[String],
    positionalNotify: (String, String) => Effect[Unit],
    namedNotify: (String, (String, String)) => Effect[Unit]
  )

  implicit def arbitraryContext: Arbitrary[Context]

  def backend: Backend[Effect]

  def run[T](effect: Effect[T]): T

  def codecFixtures: Seq[CodecFixture]

  val simpleApiInstance: SimpleApiType = SimpleApiImpl(backend)
  val complexApiInstance: ComplexApiType = ComplexApiImpl(backend)
  val invalidApiInstance: InvalidApiType = InvalidApiImpl(backend)
  val apiNames = Seq("Named", "Positional")

  "" - {
    codecFixtures.foreach { fixture =>
      fixture.codec.getSimpleName.replaceAll("Codec$", "") - {
        "Proxy" - {
          "Call" - {
            "Simple API" - {
              apiCombinations(simpleApiInstance, fixture.simpleApis).foreach { case (mode, apis) =>
                mode - {
                  "test" in {
                    check { (a0: String) =>
                      consistent(apis, (api: SimpleApiType) => api.test(a0))
                    }
                  }
                }
              }
            }
            "Complex API" - {
              apiCombinations(complexApiInstance, fixture.complexApis).foreach { case (mode, apis) =>
                mode - {
                  "method0" in {
                    check((_: Unit) => consistent(apis, (api: ComplexApiType) => api.method0()))
                  }
                  "method1" in {
                    check { (_: Unit) =>
                      consistent(apis, (api: ComplexApiType) => api.method1())
                    }
                  }
                  "method2" in {
                    check { (a0: String) =>
                      consistent(apis, (api: ComplexApiType) => api.method2(a0))
                    }
                  }
                  "method3" in {
                    check { (a0: Float, a1: Long, a2: Option[List[Int]]) =>
                      consistent(apis, (api: ComplexApiType) => api.method3(a0, a1, a2))
                    }
                  }
                  "method4" in {
                    check { (a0: BigDecimal, a1: Byte, a2: Map[String, Int], a3: Option[String]) =>
                      consistent(apis, (api: ComplexApiType) => api.method4(a0, a1, a2, a3))
                    }
                  }
                  "method5" in {
                    check { (a0: Boolean, a1: Short, a2: List[Int]) =>
                      consistent(apis, (api: ComplexApiType) => api.method5(a0, a1)(a2))
                    }
                  }
                  "method6" in {
                    check { (a0: Record, a1: Double) =>
                      consistent(apis, (api: ComplexApiType) => api.method6(a0, a1))
                    }
                  }
                  "method7" in {
                    check { (a0: Record, a1: Boolean, context: Context) =>
                      implicit val usingContext: Context = context
                      consistent(apis, (api: ComplexApiType) => api.method7(a0, a1))
                    }
                  }
                  "method8" in {
                    check { (a0: Record, a1: String, a2: Option[Double], context: Context) =>
                      implicit val usingContext: Context = context
                      consistent(apis, (api: ComplexApiType) => api.method8(a0, a1, a2))
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
                      expectedErrorMessage == result.swap.map(_.getMessage)
                    }
                  }
                }
              }
            }
            "Invalid API" - {
              apiCombinations(invalidApiInstance, fixture.invalidApis).foreach { case (mode, apis) =>
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
                    val error = intercept[InvalidResponseException] {
                      run(api.method2(""))
                    }.getMessage.toLowerCase
                    error.should(include("invalid"))
                  }
                  "Missing arguments" in {
                    val error = intercept[InvalidRequestException] {
                      run(api.method3(0, None))
                    }.getMessage.toLowerCase
                    error.should(include("argument"))
                    error.should(include("1"))
                  }
                  "Optional arguments" in {
                    run(api.method3(0, Some(0)))
                  }
                  "Invalid argument" in {
                    val error = intercept[InvalidRequestException] {
                      run(api.method4(BigDecimal(0), "", ""))
                    }.getMessage.toLowerCase
                    error.should(include("argument"))
                    error.should(include("1"))
                  }
                }
              }
            }
          }
        }
        "Direct" - {
          "Call" - {
            "Simple API" - {
              "Positional" in {
                check { (a0: String) =>
                  val expected = run(simpleApiInstance.test(a0))
                  run(fixture.positionalCall("test", a0)) == expected
                }
              }
              "Named" in {
                check { (a0: String) =>
                  val expected = run(simpleApiInstance.test(a0))
                  run(fixture.namedCall("test", "test" -> a0)) == expected
                }
              }
            }
          }
          "Notify" - {
            "Simple API" - {
              "Positional" in {
                check { (a0: String) =>
                  run(fixture.positionalNotify("test", a0))
                  true
                }
              }
              "Named" in {
                check { (a0: String) =>
                  run(fixture.namedNotify("test", "test" -> a0))
                  true
                }
              }
            }
          }
        }
      }
    }
  }

  private def apiCombinations[Api](originalApi: Api, apis: Seq[Api]): Seq[(String, (Api, Api))] =
    apis.zip(apiNames).map { case (api, name) =>
      name -> ((originalApi, api))
    }

  private def consistent[Api, Result](apis: (Api, Api), function: Api => Effect[Result]): Boolean = {
    val (referenceApi, testedApi) = apis
    val expected = run(function(referenceApi))
    val result = run(function(testedApi))
    expected == result
  }
}
