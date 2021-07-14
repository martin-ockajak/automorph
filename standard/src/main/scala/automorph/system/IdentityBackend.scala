package automorph.system

import automorph.system.IdentityBackend.Identity
import automorph.spi.EffectSystem

/**
 * Synchronous backend plugin using identity as an effect type.
 *
 * Represents direct use of computed values without wrapping them in an effect.
 *
 * Effect type: original value type
 * @constructor Creates a synchronous backend plugin using identity as an effect type.
 */
final case class IdentityBackend() extends EffectSystem[Identity] {

  override def pure[T](value: T): T = value

  override def failed[T](exception: Throwable): T = throw exception

  override def flatMap[T, R](value: T, function: T => R): R = function(value)

  override def either[T](value: T): Either[Throwable, T] = Right(value)
}

case object IdentityBackend {
  /**
   * Effect type.
   *
   * @tparam T value type
   */
  type Effect[T] = Identity[T]

  /**
   * Identity type.
   *
   * @tparam T value type
   */
  type Identity[T] = T
}
