package automorph.util

/**
 * Empty context.
 *
 * @tparam T empty context value type
 */
final case class EmptyContext[T]()

object EmptyContext {

  /** Empty context value type. */
  type Value = EmptyContext[EmptyContext.type]

  /** Implicit default empty context value. */
  implicit val default: Value = EmptyContext[EmptyContext.type]()
}
