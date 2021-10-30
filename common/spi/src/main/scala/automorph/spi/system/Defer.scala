package automorph.spi.system

/**
 * Deferred effects.
 *
 * @tparam Effect effect type
 */
trait Defer[Effect[_]] {
  /**
   * Creates a deferred effect.
   *
   * @tparam T effectful value type
   * @return deferred effect
   */
  def deferred[T]: Effect[Deferred[Effect, T]]
}
