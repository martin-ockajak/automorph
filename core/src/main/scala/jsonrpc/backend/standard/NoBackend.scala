package jsonrpc.backend.standard

import NoBackend.Identity
import jsonrpc.spi.Backend

/**
 * No effect backend plugin.
 *
 * Represents direct use of plain values without wrapping them in an effect.
 *
 * Effect type: Identity
 */
final case class NoBackend() extends Backend[Identity]:

  def pure[T](value: T): T = value

  def failed[T](exception: Throwable): T = throw exception

  def flatMap[T, R](value: T, function: T => R): R = function(value)

  def either[T](value: T): Either[Throwable, T] = Right(value)

case object NoBackend:
  type Identity[T] = T
