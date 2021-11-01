package automorph.util

import automorph.spi.EffectSystem
import scala.util.{Failure, Success, Try}

private[automorph] object Extensions {

  implicit final class ThrowableOps(private val throwable: Throwable) {

    /**
     * Assemble detailed trace of an exception and its causes.
     *
     * @param throwable exception
     * @return error messages
     */
    def trace: Seq[String] = trace(100)

    /**
     * Assemble detailed trace of an exception and its causes.
     *
     * @param throwable exception
     * @param maxCauses maximum number of included exception causes
     * @return error messages
     */
    def trace(maxCauses: Int = 100): Seq[String] =
      LazyList.iterate(Option(throwable))(_.flatMap(error => Option(error.getCause)))
        .takeWhile(_.isDefined).flatten.take(maxCauses).map { throwable =>
          val exceptionName = throwable.getClass.getSimpleName
          val message = Option(throwable.getMessage).getOrElse("")
          s"[$exceptionName] $message"
        }
  }

  implicit final class TryOps[T](private val tryValue: Try[T]) {

    /**
     * Creates a new 'Try' by applying ''onFailure'' on `Failure` or returns this on `Success`.
     *
     * @param onFailure function to apply if this is a `Failure`
     * @return a transformed `Try`
     */
    def onFailure(onFailure: Throwable => Unit): Try[T] = tryValue recoverWith { case exception =>
      onFailure(exception)
      Failure(exception)
    }

    /**
     * Applies ''onException'' on `Failure` or ''onSuccess'' on `Success`.
     *
     * @param onFailure function to apply if this is a `Success`
     * @param onSuccess function to apply if this is a `Failure`
     * @tparam U result type
     * @return applied function result
     */
    def pureFold[U](onFailure: Throwable => U, onSuccess: T => U): U = tryValue match {
      case Failure(exception) => onFailure(exception)
      case Success(value) => onSuccess(value)
    }
  }

  implicit final class EffectOps[Effect[_], T](private val effect: Effect[T]) {

    def map[R](system: EffectSystem[Effect], function: T => R): Effect[R] =
      system.map(effect, function)

    def flatMap[R](system: EffectSystem[Effect], function: T => Effect[R]): Effect[R] =
      system.flatMap(effect, function)
  }
}
