package test.core

import automorph.Types
import automorph.spi.EffectSystem
import automorph.spi.RpcProtocol.{FunctionNotFoundException, InvalidRequestException, InvalidResponseException}
import org.scalacheck.Arbitrary
import scala.util.{Failure, Success, Try}
import test.Generators.arbitraryRecord
import test.base.BaseTest
import test.{ComplexApi, ComplexApiImpl, InvalidApi, InvalidApiImpl, Record, SimpleApi, SimpleApiImpl}

/**
 * Main client -> handler RPC API function invocation test.
 *
 * Checks the results of remote RPC function invocations against identical local invocations.
 */
trait CoreTest extends BaseTest {

  /** Effect type. */
  type Effect[_]
  /** Request context type. */
  type Context
  type SimpleApiType = SimpleApi[Effect]
  type ComplexApiType = ComplexApi[Effect, Context]
  type InvalidApiType = InvalidApi[Effect]

  case class TestFixture(
    client: Types.ClientAnyCodec[Effect, Context],
    handler: Types.HandlerAnyCodec[Effect, Context],
    simpleApi: SimpleApiType,
    complexApi: ComplexApiType,
    invalidApi: InvalidApiType,
    call: (String, (String, String)) => Effect[String],
    tell: (String, (String, String)) => Effect[Unit]
  ) {

    val genericClient: Types.ClientGenericCodec[Effect, Context] =
      client.asInstanceOf[Types.ClientGenericCodec[Effect, Context]]
  }

  val simpleApi: SimpleApiType = SimpleApiImpl(system)
  val complexApi: ComplexApiType = ComplexApiImpl(system, arbitraryContext.arbitrary.sample.get)
  val invalidApi: InvalidApiType = InvalidApiImpl(system)

  implicit def arbitraryContext: Arbitrary[Context]

  def system: EffectSystem[Effect]

  def execute[T](effect: Effect[T]): T

  def fixtures: Seq[TestFixture]

  "" - {
    if (BaseTest.testBasic) {
      // Basic tests
      testFixtures.take(1).foreach { fixture =>
        "Static" - {
          "Simple API" - {
            val apis = (fixture.simpleApi, simpleApi)
            "method" in {
              consistent(apis)(_.method("value")).should(be(true))
            }
          }
        }
      }
    } else {
      // Full tests
      testFixtures.take(1).foreach { fixture =>
        //    testFixtures.foreach { fixture =>
        val codecName = fixture.genericClient.protocol.codec.getClass.getSimpleName
        codecName.replaceAll("MessageCodec$", "") - {
          "Static" - {
            "Simple API" - {
              val apis = (fixture.simpleApi, simpleApi)
              "method" in {
                check { (a0: String) =>
                  consistent(apis)(_.method(a0))
                }
              }
            }
            "Complex API" - {
              val apis = (fixture.complexApi, complexApi)
              "method0" in {
                check((_: Unit) => consistent(apis)(_.method0()))
              }
              "method1" in {
                check { (_: Unit) =>
                  consistent(apis)(_.method1())
                }
              }
              "method2" in {
                check { (a0: String) =>
                  consistent(apis)(_.method2(a0))
                }
              }
              "method3" in {
                check { (a0: Float, a1: Long, a2: Option[List[Int]]) =>
                  consistent(apis)(_.method3(a0, a1, a2))
                }
              }
              "method4" in {
                check { (a0: Double, a1: Byte, a2: Map[String, Int], a3: Option[String]) =>
                  consistent(apis)(_.method4(BigDecimal(a0), a1, a2, a3))
                }
              }
              "method5" in {
                check { (a0: Boolean, a1: Short, a2: List[Int]) =>
                  consistent(apis)(_.method5(a0, a1)(a2))
                }
              }
              "method6" in {
                check { (a0: Record, a1: Double) =>
                  consistent(apis)(_.method6(a0, a1))
                }
              }
              "method7" in {
                check { (a0: Record, a1: Boolean, context: Context) =>
                  implicit val usingContext: Context = context
                  consistent(apis)(_.method7(a0, a1))
                }
              }
              "method8" in {
                check { (a0: Record, a1: String, a2: Option[Double]) =>
                  consistent(apis) { api =>
                    system.map(api.method8(a0, a1, a2)) { result =>
                      s"${result.result} - ${result.context.getClass.getName}"
                    }
                  }
                }
              }
              "method9" in {
                check { (a0: String) =>
                  val (testedApi, referenceApi) = apis
                  val result = Try(execute(testedApi.method9(a0))).toEither
                  val expected = Try(execute(referenceApi.method9(a0))).toEither
                  val expectedErrorMessage = expected.swap.map(error =>
                    s"[${error.getClass.getSimpleName}] ${Option(error.getMessage).getOrElse("")}"
                  )
                  expectedErrorMessage == result.swap.map(_.getMessage)
                }
              }
            }
            "Invalid API" - {
              val api = fixture.invalidApi
              "Function not found" in {
                val error = intercept[FunctionNotFoundException](execute(api.nomethod(""))).getMessage.toLowerCase
                error.should(include("function not found"))
                error.should(include("nomethod"))
              }
              "Redundant arguments" in {
                val error = intercept[IllegalArgumentException](execute(api.method1(""))).getMessage.toLowerCase
                error.should(include("redundant arguments"))
                error.should(include("0"))
              }
              "Malformed result" in {
                val error = intercept[InvalidResponseException] {
                  execute(api.method2(""))
                }.getMessage.toLowerCase
                error.should(include("malformed result"))
              }
              "Optional arguments" in {
                execute(api.method3(0, Some(0)))
              }
              "Malformed argument" in {
                val error = intercept[InvalidRequestException] {
                  execute(api.method4(BigDecimal(0), Some(true), None))
                }.getMessage.toLowerCase
                error.should(include("malformed argument"))
                error.should(include("p1"))
              }
              "Missing arguments" in {
                val error = intercept[InvalidRequestException] {
                  execute(api.method5(p0 = true, 0))
                }.getMessage.toLowerCase
                error.should(include("missing argument"))
                error.should(include("p2"))
              }
            }
          }
          "Dynamic" - {
            "Simple API" - {
              "Call" in {
                check { (a0: String) =>
                  val expected = execute(simpleApi.method(a0))
                  executeLogError(fixture.call("method", "argument" -> a0)) == expected
                }
              }
              "Message" in {
                check { (a0: String) =>
                  executeLogError(fixture.tell("method", "argument" -> a0))
                  true
                }
              }
            }
          }
        }
      }
    }
  }

  override def afterAll(): Unit = {
    fixtures.foreach(_.genericClient.close())
    super.afterAll()
  }

  private def executeLogError[T](value: => Effect[T]): T =
    Try(execute(value)) match {
      case Success(result) => result
      case Failure(error) =>
        error.printStackTrace(System.out)
        throw error
    }

  private def consistent[Api, Result](apis: (Api, Api))(function: Api => Effect[Result]): Boolean =
    Try {
      val (testedApi, referenceApi) = apis
      val result = execute(function(testedApi))
      val expected = execute(function(referenceApi))
      expected == result
    } match {
      case Success(result) => result
      case Failure(error) =>
        error.printStackTrace(System.out)
        false
    }

  private def testFixtures: Seq[TestFixture] =
    fixtures.map { fixture =>
      Try(fixture).recover {
        case error =>
          error.printStackTrace(System.out)
          throw error
      }.get
    }
}
