package jsonrpc.spi

/**
 * Effectful computation system plugin.
 *
 * The underlying runtime must support monadic composition of effectful computations.
 *
 * @tparam Outcome effectful computation outcome type
 */
trait Effect[Outcome[_]]:

  /**
   * Lift a value into a new effect of given type.
   *
   * @param value an existing value
   * @tparam T effect value type
   * @return effect containing the value
   */
  def pure[T](value: T): Outcome[T]

  /**
   * Lift an exception into a new effect of given type.
   *
   * @param exception exception
   * @tparam T effect value type
   * @return effect containing the exception
   */
  def failed[T](exception: Throwable): Outcome[T]

  /**
   * Transform an effect by applying an effectful function to its value.
   *
   * @param effect effect containing a value
   * @param function function applied to the effect value returning an effect
   * @tparam T effect value type
   * @tparam R effectful function result type
   * @return effect containing the transformed value
   */
  def flatMap[T, R](effect: Outcome[T], function: T => Outcome[R]): Outcome[R]

  /**
   * Transform an effect by lifting any errors into its value.
   * The resulting effect cannot fail.
   *
   * @param value effect containing a value
   * @tparam T effect value type
   * @return effect containing an error or the original effect value
   */
  def either[T](value: Outcome[T]): Outcome[Either[Throwable, T]]

  /**
   * Transform an effect by applying a function to its value.
   *
   * @param effect effect containing a value
   * @param function function applied to the effect value
   * @tparam T effect value type
   * @tparam R function result type
   * @return effect containing the transformed value
   */
  def map[T, R](effect: Outcome[T], function: T => R): Outcome[R] = flatMap(effect, value => pure(function(value)))
