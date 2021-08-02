package test.system

import automorph.spi.EffectSystem
import scala.util.{Failure, Success, Try}
import test.base.BaseSpec

/**
 * Effect system test.
 *
 * Checks effect type operations.
 *
 * @tparam Effect effect type
 */
trait EffectSystemSpec[Effect[_]] extends BaseSpec {
  private val text = "test"
  private val number = 0

  case class TestException(message: String) extends RuntimeException(message)

  def system: EffectSystem[Effect]

  def run[T](effect: Effect[T]): Either[Throwable, T]

  "" - {
    "Pure" in {
      val outcome = system.pure(text)
      run(outcome).should(equal(Right(text)))
    }
    "Failed" in {
      Try(system.failed(TestException(text))) match {
        case Success(outcome) => run(outcome).should(equal(Left(TestException(text))))
        case Failure(error) => error.should(equal(TestException(text)))
      }
    }
    "Map" in {
      val outcome = system.map(system.pure(text), (result: String) => s"$result$number")
      run(outcome).should(equal(Right(s"$text$number")))
    }
    "Flatmap" in {
      val outcome = system.flatMap(system.pure(text), (result: String) => system.pure(s"$result$number"))
      run(outcome).should(equal(Right(s"$text$number")))
    }
    "Either" in {
      val outcome = system.either(system.pure(text))
      run(outcome).should(equal(Right(Right(text))))
    }
  }
}
