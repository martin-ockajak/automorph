package jsonrpc.effect.cats

import cats.effect.IO
import jsonrpc.spi.EffectContext

final case class CatsEffectContext[Environment]() extends EffectContext[IO]:

  def pure[T](value: T): IO[T] = IO.pure(value)

  def map[T, R](value: IO[T], function: T => R): IO[R] = value.map(function)

  def either[T](value: IO[T]): IO[Either[Throwable, T]] = value.attempt
