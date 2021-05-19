package jsonrpc.effect.native

import jsonrpc.spi.Effect
import jsonrpc.core.ScalaSupport.*
import NoEffect.*

/**
 * No effect system plugin.
 * Represents direct use of plain values without wrapping them in an effect.
 *
 * Effect type: Identity
 */
final case class NoEffect()
  extends Effect[Identity]:

  def pure[T](value: T): T = value

  def map[T, R](value: T, function: T => R): R =
    function(value)

  def either[T](value: T): Either[Throwable, T] =
    value.asRight

object NoEffect:
  type Identity[T] = T
