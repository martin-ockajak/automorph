package jsonrpc.util

/**
 * Empty value of specified type.
 *
 * @tparam T empty value type
 */
final case class Void[T]()

case object Void {

  /** Empty value type */
  type Value = Void[Void.type]

  /** Implicit empty value */
  implicit val value: Value = Void[Void.type]()
}
