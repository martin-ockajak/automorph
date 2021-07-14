package automorph.system

import cats.effect.IO
import automorph.spi.EffectSystem

/**
 * Cats Effect backend plugin.
 *
 * @see [[https://typelevel.org/cats-effect/ Documentation]]
 * @see [[https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html Effect type]]
 * @constructor Creates a Cats Effect backend plugin using `IO` as an effect type.
 */
final case class CatsEffectBackend() extends EffectSystem[IO] {

  override def pure[T](value: T): IO[T] = IO.pure(value)

  override def failed[T](exception: Throwable): IO[T] = IO.raiseError(exception)

  override def flatMap[T, R](value: IO[T], function: T => IO[R]): IO[R] = value.flatMap(function)

  override def either[T](value: IO[T]): IO[Either[Throwable, T]] = value.attempt
}

case object CatsEffectBackend {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = IO[T]
}
