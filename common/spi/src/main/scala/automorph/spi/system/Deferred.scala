package automorph.spi.system

/**
 * Possibly not yet available effectful value.
 *
 * @param effect
 *   effect containing the value
 * @param succeed
 *   completes the effect with a result value
 * @param fail
 *   completes the effect with an error
 * @tparam Effect
 *   effect type
 * @tparam T
 *   effectful value type
 */
final case class Deferred[Effect[_], T](
  effect: Effect[T],
  succeed: T => Effect[Unit],
  fail: Throwable => Effect[Unit],
)
