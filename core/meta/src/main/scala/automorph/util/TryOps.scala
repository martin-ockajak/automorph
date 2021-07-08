package automorph.util

import scala.util.{Failure, Success, Try}

implicit final class TryOps[T](private val value: Try[T]) {

  /**
   * Creates a new 'Try' by applying ''onFailure'' on `Failure` or returns this on `Success`.
   *
   * @param onFailure function to apply if this is a `Failure`
   * @return a transformed `Try`
   */
  def mapFailure(onFailure: Throwable => Throwable): Try[T] = value match {
    case Failure(exception) => Failure(onFailure(exception))
    case result => result
  }

  /**
   * Applies ''onException'' on `Failure`` or ''onSuccess'' on `Success`.
   *
   * @param onFailure function to apply if this is a `Success`
   * @param onSuccess function to apply if this is a `Failure`
   * @tparam U result type
   * @return applied function result
   */
  def pureFold[U](onFailure: Throwable => U, onSuccess: T => U): U = value match {
    case Failure(exception) => onFailure(exception)
    case Success(value) => onSuccess(value)
  }
}
