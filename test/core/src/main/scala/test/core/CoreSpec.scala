package test.core

import automorph.Handler
import automorph.client.ClientMeta
import automorph.spi.EffectSystem
import automorph.spi.RpcProtocol.{FunctionNotFoundException, InvalidRequestException, InvalidResponseException}
import com.fasterxml.jackson.databind.util.TokenBuffer
import org.scalacheck.Arbitrary
import scala.util.{Failure, Success, Try}
import test.Generators.arbitraryRecord
import test.base.BaseSpec
import test.{ComplexApi, ComplexApiImpl, InvalidApi, InvalidApiImpl, Record, SimpleApi, SimpleApiImpl}

/**
 * Main client -> handler RPC API function invocation test.
 *
 * Checks the results of remote RPC function invocations against identical local invocations.
 */
trait CoreSpec extends BaseSpec {

  /** Effect type. */
  type Effect[_]
  /** Request context type. */
  type Context
  type SimpleApiType = SimpleApi[Effect]
  type ComplexApiType = ComplexApi[Effect, Context]
  type InvalidApiType = InvalidApi[Effect]

  case class TestFixture(
    codec: Class[_],
    client: ClientMeta.AnyCodec[Effect, Context],
    handler: Handler.AnyCodec[Effect, Context],
    serverPort: Int,
    simpleApis: Seq[SimpleApiType],
    complexApis: Seq[ComplexApiType],
    invalidApis: Seq[InvalidApiType],
    positionalCall: (String, String) => Effect[String],
    namedCall: (String, (String, String)) => Effect[String],
    positionalNotify: (String, String) => Effect[Unit],
    namedNotify: (String, (String, String)) => Effect[Unit]
  )

  val simpleApiInstance: SimpleApiType = SimpleApiImpl(system)
  val complexApiInstance: ComplexApiType = ComplexApiImpl(system)
  val invalidApiInstance: InvalidApiType = InvalidApiImpl(system)
  private val apiNames = Seq("Named", "Positional")

  implicit def arbitraryContext: Arbitrary[Context]

  def system: EffectSystem[Effect]

  def run[T](effect: Effect[T]): T

  def fixtures: Seq[TestFixture]

  "" - {
// FIXME - restore
//    fixtures.foreach { fixture =>
    fixtures.headOption.foreach { fixture =>
      fixture.codec.getSimpleName.replaceAll("MessageCodec$", "") - {
//        "Proxy" - {
//          "Call" - {
//            "Simple API" - {
//              apiCombinations(simpleApiInstance, fixture.simpleApis).foreach { case (mode, apis) =>
//                mode - {
//                  "test" in {
//                    check { (a0: String) =>
//                      consistent(apis, (api: SimpleApiType) => api.test(a0))
//                    }
//                  }
//                }
//              }
//            }
//            "Complex API" - {
//              apiCombinations(complexApiInstance, fixture.complexApis).foreach { case (mode, apis) =>
//                mode - {
//                  "method0" in {
//                    check((_: Unit) => consistent(apis, (api: ComplexApiType) => api.method0()))
//                  }
//                  "method1" in {
//                    check { (_: Unit) =>
//                      consistent(apis, (api: ComplexApiType) => api.method1())
//                    }
//                  }
//                  "method2" in {
//                    check { (a0: String) =>
//                      consistent(apis, (api: ComplexApiType) => api.method2(a0))
//                    }
//                  }
//                  "method3" in {
//                    check { (a0: Float, a1: Long, a2: Option[List[Int]]) =>
//                      consistent(apis, (api: ComplexApiType) => api.method3(a0, a1, a2))
//                    }
//                  }
//                  "method4" in {
//                    check { (a0: Double, a1: Byte, a2: Map[String, Int], a3: Option[String]) =>
//                      consistent(apis, (api: ComplexApiType) => api.method4(BigDecimal(a0), a1, a2, a3))
//                    }
//                  }
//                  "method5" in {
//                    check { (a0: Boolean, a1: Short, a2: List[Int]) =>
//                      consistent(apis, (api: ComplexApiType) => api.method5(a0, a1)(a2))
//                    }
//                  }
//                  "method6" in {
//                    check { (a0: Record, a1: Double) =>
//                      consistent(apis, (api: ComplexApiType) => api.method6(a0, a1))
//                    }
//                  }
//                  "method7" in {
//                    check { (a0: Record, a1: Boolean, context: Context) =>
//                      implicit val usingContext: Context = context
//                      consistent(apis, (api: ComplexApiType) => api.method7(a0, a1))
//                    }
//                  }
//                  "method8" in {
//                    check { (a0: Record, a1: String, a2: Option[Double], context: Context) =>
//                      implicit val usingContext: Context = context
//                      consistent(apis, (api: ComplexApiType) => api.method8(a0, a1, a2))
//                    }
//                  }
//                  "method9" in {
//                    check { (a0: String) =>
//                      val (referenceApi, testedApi) = apis
//                      val expected = Try(run(referenceApi.method9(a0))).toEither
//                      val result = Try(run(testedApi.method9(a0))).toEither
//                      val expectedErrorMessage = expected.swap.map(error =>
//                        s"[${error.getClass.getSimpleName}] ${Option(error.getMessage).getOrElse("")}"
//                      )
//                      expectedErrorMessage == result.swap.map(_.getMessage)
//                    }
//                  }
//                }
//              }
//            }
//            "Invalid API" - {
//              apiCombinations(invalidApiInstance, fixture.invalidApis).foreach { case (mode, apis) =>
//                mode - {
//                  val (_, api) = apis
//                  "Function not found" in {
//                    val error = intercept[FunctionNotFoundException](run(api.nomethod(""))).getMessage.toLowerCase
//                    error.should(include("function not found"))
//                    error.should(include("nomethod"))
//                  }
//                  "Redundant arguments" in {
//                    val error = intercept[IllegalArgumentException](run(api.method1(""))).getMessage.toLowerCase
//                    error.should(include("redundant arguments"))
//                    error.should(include("0"))
//                  }
//                  "Malformed result" in {
//                    val error = intercept[InvalidResponseException] {
//                      run(api.method2(""))
//                    }.getMessage.toLowerCase
//                    error.should(include("malformed result"))
//                  }
//                  "Optional arguments" in {
//                    run(api.method3(0, Some(0)))
//                  }
//                  "Malformed argument" in {
//                    val error = intercept[InvalidRequestException] {
//                      run(api.method4(BigDecimal(0), Some(true), None))
//                    }.getMessage.toLowerCase
//                    error.should(include("malformed argument"))
//                    error.should(include("p1"))
//                  }
//                  "Missing arguments" in {
//                    val error = intercept[InvalidRequestException] {
//                      run(api.method5(true, 0))
//                    }.getMessage.toLowerCase
//                    error.should(include("missing argument"))
//                    error.should(include("p2"))
//                  }
//                }
//              }
//            }
//          }
//        }
        "Direct" - {
          "Call" - {
            "Simple API" - {
//              "Positional" in {
//                check { (a0: String) =>
//                  val expected = run(simpleApiInstance.test(a0))
//                  execute(fixture.positionalCall("test", a0)) == expected
//                }
//              }
              "Named" in {
                check { (a0: String) =>
                  val expected = run(simpleApiInstance.test(a0))
                  execute(fixture.namedCall("test", "test" -> a0)) == expected
                }
              }
            }
          }
//          "Notify" - {
//            "Simple API" - {
//              "Positional" in {
//                check { (a0: String) =>
//                  execute(fixture.positionalNotify("test", a0))
//                  true
//                }
//              }
//              "Named" in {
//                check { (a0: String) =>
//                  execute(fixture.namedNotify("test", "test" -> a0))
//                  true
//                }
//              }
//            }
//          }
        }
      }
    }
  }

  override def afterAll(): Unit = {
    fixtures.foreach(_.client.close())
    super.afterAll()
  }

  private def apiCombinations[Api](originalApi: Api, apis: Seq[Api]): Seq[(String, (Api, Api))] =
    apis.zip(apiNames).map { case (api, name) =>
      name -> ((originalApi, api))
    }

  private def execute[Result](value: => Effect[Result]): Result =
    Try(run(value)) match {
      case Success(result) => result
      case Failure(error) =>
        error.printStackTrace(System.out)
        throw error
    }

  private def consistent[Api, Result](apis: (Api, Api), function: Api => Effect[Result]): Boolean =
    Try {
      val (referenceApi, testedApi) = apis
      val expected = run(function(referenceApi))
      val result = run(function(testedApi))
      expected == result
    } match {
      case Success(result) => result
      case Failure(error) =>
        error.printStackTrace(System.out)
        false
    }
}
