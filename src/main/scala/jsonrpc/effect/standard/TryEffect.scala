package jsonrpc.effect.native

import jsonrpc.spi.Effect
import scala.util.{Success, Try as E}

final case class EEffect()
  extends Effect[E]:
  
  def pure[T](value: T): E[T] =
    Success(value)

  def map[T, R](value: E[T], function: T => R): E[R] =
    value.map(function)

  def either[T](value: E[T]): E[Either[Throwable, T]] =
    Success(value.toEither)
