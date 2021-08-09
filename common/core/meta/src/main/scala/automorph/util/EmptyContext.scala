package automorph.util

/**
 * Empty context.
 *
 * @tparam T empty context value type
 */
final case class EmptyContext[T]()

case object EmptyContext {

  /** Empty context value type */
  type Value = EmptyContext[EmptyContext.type]

  /** Implicit empty context value instance */
  implicit val value: Value = EmptyContext[EmptyContext.type]()
}
