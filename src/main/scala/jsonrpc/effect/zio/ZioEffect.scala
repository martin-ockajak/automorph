package jsonrpc.effect.zio

import jsonrpc.spi.Effect
import zio.{RIO as E, Task}

final case class ZioEffect[Environment]()
  extends Effect[[T] =>> E[Environment, T]]:

  def pure[T](value: T): E[Environment, T] =
    E.succeed(value)

  def map[T, R](value: E[Environment, T], function: T => R): E[Environment, R] =
    value.map(function)

  def either[T](value: E[Environment, T]): E[Environment, Either[Throwable, T]] =
    value.either
