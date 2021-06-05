package jsonrpc.backend

import base.BaseSpec
import jsonrpc.spi.Backend
import scala.util.{Failure, Success, Try}

trait BackendSpec[Effect[_]] extends BaseSpec:
  private val text = "test"
  private val number = 0

  def effect: Backend[Effect]

  def run[T](outcome: Effect[T]): Either[Throwable, T]

  case class TestException(message: String) extends RuntimeException(message) derives CanEqual

  "" - {
    "Pure" in {
      val outcome = effect.pure(text)
      run(outcome).should(equal(Right(text)))
    }
    "Failed" in {
      Try(effect.failed(TestException(text))) match
        case Success(outcome) => run(outcome).should(equal(Left(TestException(text))))
        case Failure(error) => error.should(equal(TestException(text)))
    }
    "Map" in {
      val outcome = effect.map(effect.pure(text), result => s"$result$number")
      run(outcome).should(equal(Right(s"$text$number")))
    }
    "Flatmap" in {
      val outcome = effect.flatMap(effect.pure(text), result => effect.pure(s"$result$number"))
      run(outcome).should(equal(Right(s"$text$number")))
    }
    "Either" in {
      val outcome = effect.either(effect.pure(text))
      run(outcome).should(equal(Right(Right(text))))
    }
  }
