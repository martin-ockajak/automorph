package jsonrpc.effect.scalaz

import jsonrpc.spi.Effect
import scalaz.effect.IO

/**
 * FS2 effect system plugin.
 *
 * @see [[https://github.com/scalaz/ioeffect Documentation]]
 * @see [[https://javadoc.io/doc/org.scalaz/scalaz-effect_2.12/latest/scalaz/effect/IO.html Effect type]]
 */
final case class ScalazEffect() extends Effect[IO]:

  def pure[T](value: T): IO[T] = IO(value)

  def failed[T](exception: Throwable): IO[T] = IO.throwIO(exception)

  def flatMap[T, R](value: IO[T], function: T => IO[R]): IO[R] = value.flatMap(function)

  def either[T](value: IO[T]): IO[Either[Throwable, T]] = value.catchLeft.map(_.toEither)
