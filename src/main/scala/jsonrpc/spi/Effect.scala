package jsonrpc.spi

/**
 * Computation effect system plugin.
 *
 * @tparam Outcome computation outcome effect type
 */
trait Effect[Outcome[_]]:
  /**
   * Lift a value into a new effect.
   *
   * @param value already computed value
   * @tparam T value type
   * @return effect containing the value
   */
  def pure[T](value: T): Outcome[T]

  /**
   * Transform an effect by applying a function to its value.
   *
   * @param effect effect containing a value
   * @param function function applied to the effect value
   * @tparam T effect value type
   * @tparam R function result type
   * @return effect containing the transformed value
   */
  def map[T, R](effect: Outcome[T], function: T => R): Outcome[R]

  /**
   * Transform an effect by lifting any errors into its value.
   * The resulting effect cannot fail.
   *
   * @param value effect containing a value
   * @tparam T effect value type
   * @return effect containing an error or the original effect value
   */
  def either[T](value: Outcome[T]): Outcome[Either[Throwable, T]]
