package automorph.system

import cats.effect.IO
import automorph.spi.EffectSystem

/**
 * Cats Effect effect system plugin.
 *
 * @see [[https://typelevel.org/cats-effect/ Library documentation]]
 * @see [[https://typelevel.org/cats-effect/api/3.x/cats/effect/IO.html Effect type]]
 * @constructor Creates a Cats Effect effect system plugin using `IO` as an effect type.
 */
final case class CatsEffectSystem() extends EffectSystem[IO] {

  override def wrap[T](value: => T): IO[T] =
    IO(value)

  override def pure[T](value: T): IO[T] =
    IO.pure(value)

  override def failed[T](exception: Throwable): IO[T] =
    IO.raiseError(exception)

  override def either[T](effect: IO[T]): IO[Either[Throwable, T]] =
    effect.attempt

  override def flatMap[T, R](effect: IO[T], function: T => IO[R]): IO[R] =
    effect.flatMap(function)
}

object CatsEffectSystem {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = IO[T]
}
