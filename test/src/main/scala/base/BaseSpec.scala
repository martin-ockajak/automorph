package base

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AppendedClues, BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.Checkers

/**
 * Base hierarchical test.
 *
 * Included functionality:
 * - optional values retrieval support
 * - before and after test hooks
 * - result assertion matchers
 * - additional test clues
 * - property-based checks
 * - managed auto-releasing fixtures
 * - asynchronous values retrieval
 * - free network port detection
 */
trait BaseSpec
  extends AnyFreeSpecLike
  with OptionValues
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with Matchers
  with AppendedClues
  with Checkers
  with Fixtures
  with Await
  with Network
