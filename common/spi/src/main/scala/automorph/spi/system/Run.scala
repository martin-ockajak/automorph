package automorph.spi.system

/**
 * Runnable effects.
 *
 * @tparam Effect effect type
 */
trait Run[Effect[_]] {
  /**
   * Executes an effect asynchronously without blocking.
   *
   * @tparam T effectful value type
   * @return nothing
   */
  def run[T](effect: Effect[T]): Unit
}
