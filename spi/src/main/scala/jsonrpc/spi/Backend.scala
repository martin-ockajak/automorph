package jsonrpc.spi

/**
 * Effectful computation backend plugin.
 *
 * The underlying runtime must support monadic composition of effects.
 *
 * @tparam Effect effect type (similar to IO Monad in Haskell)
 */
trait Backend[Effect[_]] {

  /**
   * Lift a value into a new effect of given type.
   *
   * @param value an existing value
   * @tparam T effectful value type
   * @return effect containing the value
   */
  def pure[T](value: T): Effect[T]

  /**
   * Lift an exception into a new effect of given type.
   *
   * @param exception exception
   * @tparam T effectful value type
   * @return effect containing the exception
   */
  def failed[T](exception: Throwable): Effect[T]

  /**
   * Transform an effect by applying an effectful function to its value.
   *
   * @param value effectful value
   * @param function function applied to the effectful value returning a new effect
   * @tparam T effectful value type
   * @tparam R effectful function result type
   * @return effect containing the transformed value
   */
  def flatMap[T, R](value: Effect[T], function: T => Effect[R]): Effect[R]

  /**
   * Transform an effect by lifting any errors into its value.
   *
   * The resulting effect cannot fail.
   *
   * @param value effectful value
   * @tparam T effectful value type
   * @return effectful error or the original value
   */
  def either[T](value: Effect[T]): Effect[Either[Throwable, T]]

  /**
   * Transform an effect by applying a function to its value.
   *
   * @param effect effectful value
   * @param function function applied to the effectful value
   * @tparam T effectful value type
   * @tparam R function result type
   * @return effectful transformed value
   */
  def map[T, R](effect: Effect[T], function: T => R): Effect[R] =
    flatMap(effect, (value: T) => pure(function(value)))
}