package jsonrpc.util

/**
 * Empty context.
 *
 * @tparam T empty context value type
 */
final case class NoContext[T]()

case object NoContext {

  /** Empty context value type */
  type Value = NoContext[NoContext.type]

  /** Implicit empty context value instance */
  implicit val value: Value = NoContext[NoContext.type]()
}
