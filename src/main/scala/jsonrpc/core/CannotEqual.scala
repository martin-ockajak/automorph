package jsonrpc.core

import jsonrpc.core.ValueOps.classNameSimple

/**
 * This is a marker trait for classes containing members for which equality is buggy
 * (typically functions: they implement equality as identity [operator eq], which yields wrong results).
 * Scala at some point will introduce automatic CanEqual type class derivation for case classes, case objects and enums:
 * with that, some opt-out annotation @CannotEqual will follow, to prevent synthetic type class derivation of CanEqual.
 * This trait is a temporary placeholder for the (yet to come) @CannotEqual annotation.
 * At the same time, it prevents comparison at runtime, if universal equality is applied.
 */
trait CannotEqual:
  override def equals(that:Any):Boolean =
    sys.error(s"instance of $classNameSimple cannot be compared with == or !=, because the class contains members with faulty equality")
