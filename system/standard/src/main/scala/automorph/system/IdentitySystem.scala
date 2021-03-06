package automorph.system

import automorph.spi.EffectSystem
import automorph.system.IdentitySystem.Identity
import scala.util.Try

/**
 * Synchronous effect system plugin using identity as an effect type.
 *
 * Represents direct use of computed values without wrapping them in an effect.
 *
 * @see [[https://www.javadoc.io/doc/org.automorph/automorph-standard_2.13/latest/automorph/system/IdentitySystem$$Identity.html Effect type]]
 * @constructor Creates a synchronous effect system plugin using identity as an effect type.
 */
final case class IdentitySystem() extends EffectSystem[Identity] {

  override def wrap[T](value: => T): T =
    value

  override def pure[T](value: T): T =
    value

  override def failed[T](exception: Throwable): T =
    throw exception

  override def either[T](effect: => T): Either[Throwable, T] =
    Try(effect).toEither

  override def flatMap[T, R](effect: T)(function: T => R): R =
    function(effect)

  override def run[T](effect: T): Unit =
    ()
}

object IdentitySystem {

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
