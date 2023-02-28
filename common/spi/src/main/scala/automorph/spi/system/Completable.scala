package automorph.spi.system

/**
 * Externally completable effect.
 *
 * @tparam Effect
 *   effect type
 * @tparam T
 *   effectful value type
 */
trait Completable[Effect[_], T] {
  /**
   * Effect containing an externally supplied result value or an exception.
   *
   * @return effect containing an externally supplied result value or an exception
   */
  def effect: Effect[T]

  /**
   * Completes the effect with a result value.
   *
   * @param value
   *   result value
   * @return
   *   nothing
   */
  def succeed(value: T): Effect[Unit]

  /**
   * Completes the effect with an exception.
   *
   * @param exception
   *   exception
   * @return
   *   nothing
   */
  def fail(exception: Throwable): Effect[Unit]
}
