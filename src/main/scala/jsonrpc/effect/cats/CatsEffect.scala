package jsonrpc.effect.cats

import cats.effect.IO
import jsonrpc.spi.Effect

final case class CatsEffect[Environment]()
  extends Effect[IO]:

  def pure[T](value: T): IO[T] =
    IO.pure(value)

  def map[T, R](value: IO[T], function: T => R): IO[R] =
    value.map(function)

  def either[T](value: IO[T]): IO[Either[Throwable, T]] =
    value.attempt
