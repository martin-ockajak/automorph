package automorph.util

/**
 * RPC request context.
 *
 * @tparam T request context value type
 */
final case class Context[T]()

object Context {

  /** Empty request context value type. */
  type Empty = Context[Context.type]

  /** Implicit empty request context value. */
  implicit val empty: Empty = Context[Context.type]()
}
