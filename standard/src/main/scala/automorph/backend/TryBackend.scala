package automorph.backend

import automorph.spi.EffectSystem
import scala.util.{Failure, Success, Try}

/**
 * Synchronous backend plugin using `Try` as an effect type.
 *
 * @see [[https://docs.scala-lang.org/overviews/scala-book/functional-error-handling.html Documentation]]
 * @see Effect type: [[scala.util.Try]]
 * @constructor Creates a synchronous backend plugin using ''Try'' as an effect type.
 */
final case class TryBackend() extends EffectSystem[Try] {

  override def pure[T](value: T): Try[T] = Success(value)

  override def failed[T](exception: Throwable): Try[T] = Failure(exception)

  override def flatMap[T, R](effect: Try[T], function: T => Try[R]): Try[R] = effect.flatMap(function)

  override def either[T](value: Try[T]): Try[Either[Throwable, T]] = Success(value.toEither)
}

case object TryBackend {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = Try[T]
}
