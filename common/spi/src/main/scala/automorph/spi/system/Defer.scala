package automorph.spi.system

/**
 * Deferrred effects support.
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
