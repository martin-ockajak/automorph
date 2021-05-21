package jsonrpc.core

import jsonrpc.core.ValueOps.classNameSimple

/**
 * This prevents comparison for case classes containing members with faulty equality.
 * (such members typically are functions: functions implement equality as identity [operator eq], which yields wrong results).
 * In case universalEquality applies, this prevents comparison at runtime (better than wrong results).
 * In case strictEquality one day becomes the default, following can apply:
 *   - if the compiler automatically infers CanEqual derivation only when all members derive from CanEqual (functions will not), this trait can be removed.
 *   - if they introduce some @CannotEqual annotation, to manually disable CanEqual derivation for case classes, this trait can be replaced with @CannotEqual.
 */
trait CannotEqual:
  override def equals(that:Any):Boolean =
    sys.error(s"instance of $classNameSimple cannot be compared with == or !=")
