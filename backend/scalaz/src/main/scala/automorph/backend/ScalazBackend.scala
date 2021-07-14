package automorph.backend

import automorph.spi.EffectSystem
import scalaz.effect.IO

/**
 * Scalaz backend plugin using `IO` as an effect type.
 *
 * @see [[https://github.com/scalaz Documentation]]
 * @see [[https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/latest/scalaz/effect/IO.html Effect type]]
 * @constructor Creates a Scalaz backend plugin using `IO` as an effect type.
 */
final case class ScalazBackend() extends EffectSystem[IO] {

  override def pure[T](value: T): IO[T] = IO(value)

  override def failed[T](exception: Throwable): IO[T] = IO.throwIO(exception)

  override def flatMap[T, R](value: IO[T], function: T => IO[R]): IO[R] = value.flatMap(function)

  override def either[T](value: IO[T]): IO[Either[Throwable, T]] = value.catchLeft.map(_.toEither)
}

case object ScalazBackend {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = IO[T]
}
