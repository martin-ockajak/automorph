package jsonrpc.effect.standard

import jsonrpc.effect.standard.NoEffect.Identity
import jsonrpc.spi.Effect

/**
 * No effect system plugin.
 * Represents direct use of plain values without wrapping them in an effect.
 *
 * Effect type: Identity
 */
final case class NoEffect() extends Effect[Identity]:

  def pure[T](value: T): T = value

  def failed[T](exception: Throwable): T = throw exception

  def flatMap[T, R](value: T, function: T => R): R = function(value)

  def either[T](value: T): Either[Throwable, T] = Right(value)

case object NoEffect:
  type Identity[T] = T
