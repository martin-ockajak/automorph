package automorph.util

import automorph.spi.EffectSystem
import scala.util.{Failure, Success, Try}

private[automorph] object Extensions {

  implicit final class ThrowableOps(private val throwable: Throwable) {

    /**
     * Assemble detailed trace of an exception and its causes.
     *
     * @return error messages
     */
    def trace: Seq[String] = trace()

    /**
     * Assemble detailed trace of an exception and its causes.
     *
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
    def onFailure(onFailure: Throwable => Unit): Try[T] = tryValue.recoverWith { case error =>
      onFailure(error)
      Failure(error)
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
      case Failure(error) => onFailure(error)
      case Success(value) => onSuccess(value)
    }
  }

  implicit final class EffectOps[Effect[_], T](private val effect: Effect[T]) {

    /**
     * Creates a new effect by lifting an effect's errors into a value.
     *
     * The resulting effect cannot fail.
     *
     * @return effectful error or the original value
     */
    def either(implicit system: EffectSystem[Effect]): Effect[Either[Throwable, T]] =
      system.either(effect)

    /**
     * Creates a new effect by applying a function to an effect's value.
     *
     * @param function function applied to the specified effect's value
     * @tparam R function result type
     * @return transformed effectful value
     */
    def map[R](function: T => R)(implicit system: EffectSystem[Effect]): Effect[R] =
      system.map(effect)(function)

    /**
     * Creates a new effect by applying an effectful function to an effect's value.
     *
     * @param function effectful function applied to the specified effect's value
     * @tparam R effectful function result type
     * @return effect containing the transformed value
     */
    def flatMap[R](function: T => Effect[R])(implicit system: EffectSystem[Effect]): Effect[R] =
      system.flatMap(effect)(function)

    /**
     * Executes an effect asynchronously without blocking.
     *
     * @return nothing
     */
    def run(implicit system: EffectSystem[Effect]): Unit =
      system.run(effect)
  }
}
