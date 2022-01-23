package automorph.util

/**
 * Empty RPC request context.
 *
 * @tparam T empty request context value type
 */
final case class EmptyContext[T]()

object EmptyContext {

  /** Empty request context value type. */
  type Value = EmptyContext[EmptyContext.type]

  /** Implicit empty request context value. */
  implicit val value: Value = EmptyContext[EmptyContext.type]()
}
