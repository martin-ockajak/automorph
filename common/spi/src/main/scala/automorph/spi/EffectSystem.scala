package automorph.spi

/**
 * Computational effect system plugin.
 *
 * The underlying runtime must support monadic composition of effectful values.
 *
 * @tparam Effect effect type (similar to IO Monad in Haskell)
 */
trait EffectSystem[Effect[_]] {

  /**
   * Lifts a potentially blocking side-effecting value into a new effect of specified type.
   *
   * Exceptions thrown while computing the value are translated into a failed effect.
   *
   * @param value side-effecting value
   * @tparam T effectful value type
   * @return effect containing the value
   */
  def wrap[T](value: => T): Effect[T]

  /**
   * Lifts a pure value into a new effect of specified type.
   *
   * @param value pure value
   * @tparam T effectful value type
   * @return effect containing the value
   */
  def pure[T](value: T): Effect[T]

  /**
   * Lifts an exception into a new effect of specified type.
   *
   * @param exception exception
   * @tparam T effectful value type
   * @return effect containing the exception
   */
  def failed[T](exception: Throwable): Effect[T]

  /**
   * Creates a new effect by lifting an effect's errors into a value.
   *
   * The resulting effect cannot fail.
   *
   * @param effect effectful value
   * @tparam T effectful value type
   * @return effectful error or the original value
   */
  def either[T](effect: Effect[T]): Effect[Either[Throwable, T]]

  /**
   * Creates a new effect by applying an effectful function to an effect's value.
   *
   * @param effect effectful value
   * @param function effectful function applied to the specified effect's value
   * @tparam T effectful value type
   * @tparam R effectful function result type
   * @return effect containing the transformed value
   */
  def flatMap[T, R](effect: Effect[T], function: T => Effect[R]): Effect[R]

  /**
   * Creates a new effect by applying a function to an effect's value.
   *
   * @param effect effectful value
   * @param function function applied to the specified effect's value
   * @tparam T effectful value type
   * @tparam R function result type
   * @return transformed effectful value
   */
  def map[T, R](effect: Effect[T], function: T => R): Effect[R] =
    flatMap(effect, (value: T) => pure(function(value)))

  /**
   * Executes an effect asynchronously without blocking.
   *
   * @param effect effectful value
   * @tparam T effectful value type
   * @return nothing
   */
  def run[T](effect: Effect[T]): Unit
}
