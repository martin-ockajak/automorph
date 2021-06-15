package jsonrpc.backend

import jsonrpc.spi.Backend
import scalaz.effect.IO

/**
 * FS2 effect backend plugin.
 *
 * @see [[https://github.com/scalaz/ioeffect Documentation]]
 * @see [[https://javadoc.io/doc/org.scalaz/scalaz-effect_2.12/latest/scalaz/effect/IO.html Effect type]]
 */
final case class ScalazBackend() extends Backend[IO] {

  override def pure[T](value: T): IO[T] = IO(value)

  override def failed[T](exception: Throwable): IO[T] = IO.throwIO(exception)

  override def flatMap[T, R](value: IO[T], function: T => IO[R]): IO[R] = value.flatMap(function)

  override def either[T](value: IO[T]): IO[Either[Throwable, T]] = value.catchLeft.map(_.toEither)
}
