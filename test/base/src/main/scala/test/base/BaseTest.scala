package test.base

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AppendedClues, BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.{Checkers, ScalaCheckPropertyChecks}

/**
 * Base structured test.
 *
 * Included functionality:
 *   - optional values retrieval support
 *   - before and after test hooks
 *   - result assertion matchers
 *   - additional test clues
 *   - property-based checks
 *   - asynchronous values retrieval
 *   - free network port detection
 */
trait BaseTest
  extends AnyFreeSpecLike
  with OptionValues
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with Matchers
  with AppendedClues
  with Checkers
  with ScalaCheckPropertyChecks

case object BaseTest {

  /** Enable basic tests only environment variable. */
  private val testBasicEnvironment = "TEST_BASIC"

  /** Basic tests enabled only. */
  final def testBasic: Boolean =
    Option(System.getenv(testBasicEnvironment)).isDefined
}
