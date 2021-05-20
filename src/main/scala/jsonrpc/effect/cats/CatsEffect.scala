package jsonrpc.effect.cats

import cats.effect.IO
import jsonrpc.spi.Effect

/**
 * Cats Effect effect system plugin.
 *
 * Documentation: https://typelevel.org/cats-effect/
 * Effect type: IO
 * Effect type API: https://www.javadoc.io/doc/org.typelevel/cats-effect_3/latest/cats/effect/IO.html
 */
final case class CatsEffect[Environment]()
  extends Effect[IO]:

  def pure[T](value: T): IO[T] = IO.pure(value)

  def map[T, R](value: IO[T], function: T => R): IO[R] = value.map(function)

  def either[T](value: IO[T]): IO[Either[Throwable, T]] = value.attempt
