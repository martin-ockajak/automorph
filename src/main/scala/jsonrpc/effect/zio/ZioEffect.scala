package jsonrpc.effect.zio

import jsonrpc.spi.Effect
import zio.{RIO, Task}

final case class ZioEffect[Environment]()
  extends Effect[[T] =>> RIO[Environment, T]]:

  def pure[T](value: T): RIO[Environment, T] =
    RIO.succeed(value)

  def map[T, R](value: RIO[Environment, T], function: T => R): RIO[Environment, R] =
    value.map(function)

  def either[T](value: RIO[Environment, T]): RIO[Environment, Either[Throwable, T]] =
    value.either
