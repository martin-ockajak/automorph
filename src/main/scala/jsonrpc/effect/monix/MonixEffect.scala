package jsonrpc.effect.monix

import jsonrpc.spi.Effect
import monix.eval.{Task as E}

final case class MonixEffect[Environment]()
  extends Effect[E]:

  def pure[T](value: T): E[T] =
    E.pure(value)

  def map[T, R](value: E[T], function: T => R): E[R] =
    value.map(function)

  def either[T](value: E[T]): E[Either[Throwable, T]] =
    value.attempt
