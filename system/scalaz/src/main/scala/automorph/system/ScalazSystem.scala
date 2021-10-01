package automorph.system

import automorph.spi.EffectSystem
import scalaz.effect.IO

/**
 * Scalaz backend plugin using `IO` as an effect type.
 *
 * @see [[https://github.com/scalaz Documentation]]
 * @see [[https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html Effect type]]
 * @constructor Creates a Scalaz backend plugin using `IO` as an effect type.
 */
final case class ScalazSystem() extends EffectSystem[IO] {

  override def wrap[T](value: => T): IO[T] = IO(value)

  override def pure[T](value: T): IO[T] = IO(value)

  override def failed[T](exception: Throwable): IO[T] = IO.throwIO(exception)

  override def either[T](effect: IO[T]): IO[Either[Throwable, T]] = effect.catchLeft.map(_.toEither)

  override def flatMap[T, R](effect: IO[T], function: T => IO[R]): IO[R] = effect.flatMap(function)
}

object ScalazSystem {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = IO[T]
}
