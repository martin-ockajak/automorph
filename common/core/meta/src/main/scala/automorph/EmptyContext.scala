package automorph

/**
 * Empty RPC message context.
 *
 * @tparam T empty RPC message context value type
 */
final case class EmptyContext[T]()

object EmptyContext {

  /** Empty RPC message context value type. */
  type Value = EmptyContext[EmptyContext.type]

  /** Implicit empty RPC message context value. */
  implicit val value: Value = EmptyContext[EmptyContext.type]()
}
