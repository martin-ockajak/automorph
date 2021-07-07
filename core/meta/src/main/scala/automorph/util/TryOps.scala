package automorph.util

import scala.util.{Failure, Try}

implicit final class TryOps[T](private val value: Try[T]) {

  def mapFailure(mapFailure: Throwable => Throwable): Try[T] = value match {
    case Failure(error) => Failure(mapFailure(error))
    case result => result
  }
}
