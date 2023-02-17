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

  case class TestException(message: String) extends RuntimeException(message)

  val text = "test"
  val number = 0
  val error: TestException = TestException(text)

  def system: EffectSystem[Effect]

  def run[T](effect: Effect[T]): Either[Throwable, T]

  "" - {
    "Evaluate" - {
      "Success" in {
        val effect = system.evaluate(text)
        run(effect).should(equal(Right(text)))
      }
      "Failure" in {
        Try(system.evaluate(throw error)) match {
          case Success(effect) => run(effect).should(equal(Left(error)))
          case Failure(error) => error.should(equal(error))
        }
      }
    }
    "Pure" in {
      val effect = system.pure(text)
      run(effect).should(equal(Right(text)))
    }
    "Error" in {
      Try(system.error(error)) match {
        case Success(effect) => run(effect).should(equal(Left(error)))
        case Failure(error) => error.should(equal(error))
      }
    }
    "Map" - {
      "Success" in {
        val effect = system.map(system.pure(text))(result => s"$result$number")
        run(effect).should(equal(Right(s"$text$number")))
      }
      "Failure" in {
        Try(system.map(system.error(error))(_ => ())) match {
          case Success(effect) => run(effect).should(equal(Left(error)))
          case Failure(error) => error.should(equal(error))
        }
      }
    }
    "Flatmap" - {
      "Success" in {
        val effect = system.flatMap(system.pure(text))(result => system.pure(s"$result$number"))
        run(effect).should(equal(Right(s"$text$number")))
      }
      "Failure" in {
        Try(system.flatMap(system.error(error))(_ => system.pure(()))) match {
          case Success(effect) => run(effect).should(equal(Left(error)))
          case Failure(error) => error.should(equal(error))
        }
      }
    }
    "Either" - {
      "Success" in {
        val effect = system.either(system.pure(text))
        run(effect).should(equal(Right(Right(text))))
      }
      "Failure" in {
        Try(system.either(system.error(error))) match {
          case Success(effect) => run(effect).should(equal(Right(Left(error))))
          case Failure(error) => error.should(equal(error))
        }
      }
    }
    "Fork" in {
      system.fork(system.evaluate(text))
    }
  }
}
