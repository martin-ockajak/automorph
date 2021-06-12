package jsonrpc.util

/**
 * Prevents comparison for case classes containing members with incorrect equality.
 *
 * Such members are typically which implement equality as identity via eq method yielding incorrect results.
 * With universalEquality compiler feature enabled the comparisons are prevented at runtime.
 * With strictEquality compiler feature enabled the comparison are prevented at compile time and the following may apply:
 * - if the compiler automatically derives CanEqual only if all type members implement CanEqual (functions will not), this trait can be removed.
 * - if a new mechanism is provided to selectively disable CanEqual derivation, this trait can be replaced with that mechanism.
 */
trait CannotEqual {
  override def equals(that: Any): Boolean =
    sys.error(s"Instances of ${this.getClass.getName} cannot be compared with == or !=")
}
