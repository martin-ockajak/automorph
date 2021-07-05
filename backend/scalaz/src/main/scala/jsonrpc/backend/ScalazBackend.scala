package jsonrpc.backend

import jsonrpc.spi.Backend
import scalaz.effect.IO

/**
 * Scalaz effect backend plugin.
 *
 * @see [[https://github.com/scalaz Documentation]]
 * @see [[https://www.javadoc.io/doc/org.scalaz/scalaz_2.13/7.4.0/scalaz/effect/IO.html Effect type]]
 */
final case class ScalazBackend() extends Backend[IO] {

  override def pure[T](value: T): IO[T] = IO(value)

  override def failed[T](exception: Throwable): IO[T] = IO.throwIO(exception)

  override def flatMap[T, R](value: IO[T], function: T => IO[R]): IO[R] = value.flatMap(function)

  override def either[T](value: IO[T]): IO[Either[Throwable, T]] = value.catchLeft.map(_.toEither)
}
