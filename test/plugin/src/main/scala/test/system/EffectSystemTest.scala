package test.system

import automorph.spi.EffectSystem
import scala.util.{Failure, Success, Try}
import test.base.BaseTest

/**
 * Effect system test.
 *
 * @tparam Effect effect type
 */
trait EffectSystemTest[Effect[_]] extends BaseTest {

  val text = "test"
  val number = 0
  val error = TestException(text)

  case class TestException(message: String) extends RuntimeException(message)

  def system: EffectSystem[Effect]

  def execute[T](effect: Effect[T]): Either[Throwable, T]

  "" - {
    "Wrap" - {
      "Success" in {
        val outcome = system.wrap(text)
        execute(outcome).should(equal(Right(text)))
      }
      "Failure" in {
        Try(system.wrap(throw error)) match {
          case Success(outcome) => execute(outcome).should(equal(Left(error)))
          case Failure(error) => error.should(equal(error))
        }
      }
    }
    "Pure" in {
      val outcome = system.pure(text)
      execute(outcome).should(equal(Right(text)))
    }
    "Failed" in {
      Try(system.failed(error)) match {
        case Success(outcome) => execute(outcome).should(equal(Left(error)))
        case Failure(error) => error.should(equal(error))
      }
    }
    "Map" - {
      "Success" in {
        val outcome = system.map(system.pure(text), (result: String) => s"$result$number")
        execute(outcome).should(equal(Right(s"$text$number")))
      }
      "Failure" in {
        Try(system.map(system.failed(error), (_: Unit) => ())) match {
          case Success(outcome) => execute(outcome).should(equal(Left(error)))
          case Failure(error) => error.should(equal(error))
        }
      }
    }
    "Flatmap" - {
      "Success" in {
        val outcome = system.flatMap(system.pure(text), (result: String) => system.pure(s"$result$number"))
        execute(outcome).should(equal(Right(s"$text$number")))
      }
      "Failure" in {
        Try(system.flatMap(system.failed(error), (_: Unit) => system.pure(()))) match {
          case Success(outcome) => execute(outcome).should(equal(Left(error)))
          case Failure(error) => error.should(equal(error))
        }
      }
    }
    "Either" - {
      "Success" in {
        val outcome = system.either(system.pure(text))
        execute(outcome).should(equal(Right(Right(text))))
      }
      "Failure" in {
        Try(system.either(system.failed(error))) match {
          case Success(outcome) => execute(outcome).should(equal(Right(Left(error))))
          case Failure(error) => error.should(equal(error))
        }
      }
    }
    "Run" in {
      system.run(system.wrap(text))
    }
  }
}
