package jsonrpc.backend

import cats.effect.IO
import jsonrpc.spi.Backend

/**
 * Cats Effect effect backend plugin.
 *
 * @see [[https://typelevel.org/cats-effect/ Documentation]]
 * @see [[https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html Effect type]]
 */
final case class CatsBackend() extends Backend[IO] {

  override def pure[T](value: T): IO[T] = IO.pure(value)

  override def failed[T](exception: Throwable): IO[T] = IO.raiseError(exception)

  override def flatMap[T, R](value: IO[T], function: T => IO[R]): IO[R] = value.flatMap(function)

  override def either[T](value: IO[T]): IO[Either[Throwable, T]] = value.attempt
}
