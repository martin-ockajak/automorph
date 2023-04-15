package test.system

import automorph.spi.EffectSystem
import test.base.BaseTest

/**
 * Effect system test.
 *
 * @tparam Effect effect type
 */
trait EffectSystemTest[Effect[_]] extends BaseTest {

  sealed case class TestException(message: String) extends RuntimeException(message)

  val text = "test"
  val number = 0
  val error: TestException = TestException(text)

  def system: EffectSystem[Effect]

  def run[T](effect: Effect[T]): Either[Throwable, T]

  "" - {
    "Evaluate" - {
      "Success" in {
        val effect = system.evaluate(text)
        run(effect).shouldEqual(Right(text))
      }
      "Failure" in {
        val effect = system.evaluate(throw error)
        run(effect).shouldEqual(Left(error))
      }
    }
    "Successful" in {
      val effect = system.successful(text)
      run(effect).shouldEqual(Right(text))
    }
    "Failed" in {
      val effect = system.failed(error)
      run(effect).shouldEqual(Left(error))
    }
    "Map" - {
      "Success" in {
        val effect = system.map(system.successful(text))(result => s"$result$number")
        run(effect).shouldEqual(Right(s"$text$number"))
      }
      "Failure" in {
        val effect = system.map(system.failed(error))(_ => ())
        run(effect).shouldEqual(Left(error))
      }
    }
    "FlatMap" - {
      "Success" in {
        val effect = system.flatMap(system.successful(text))(result => system.successful(s"$result$number"))
        run(effect).shouldEqual(Right(s"$text$number"))
      }
      "Failure" in {
        val effect = system.flatMap(system.failed(error))(_ => system.successful {})
        run(effect).shouldEqual(Left(error))
      }
    }
    "Either" - {
      "Success" in {
        val effect = system.either(system.successful(text))
        run(effect).shouldEqual(Right(Right(text)))
      }
      "Failure" in {
        val effect = system.either(system.failed(error))
        run(effect).shouldEqual(Right(Left(error)))
      }
    }
    "RunAsync" in {
      system.runAsync(system.evaluate(text))
    }
  }
}
