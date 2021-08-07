package automorph.system

import automorph.system.IdentitySystem.Identity
import automorph.spi.EffectSystem

/**
 * Synchronous backend plugin using identity as an effect type.
 *
 * Represents direct use of computed values without wrapping them in an effect.
 *
 * Effect type: original value type
 * @constructor Creates a synchronous backend plugin using identity as an effect type.
 */
final case class IdentitySystem() extends EffectSystem[Identity] {

  override def pure[T](value: T): T = value

  override def failed[T](exception: Throwable): T = throw exception

  override def flatMap[T, R](effect: T, function: T => R): R = function(effect)

  override def either[T](effect: T): Either[Throwable, T] = Right(effect)
}

case object IdentitySystem {
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
