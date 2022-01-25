package test.base

import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AppendedClues, BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.Checkers
import test.base.BaseTest.testBasicEnvironment

/**
 * Base structured test.
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
trait BaseTest
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

object BaseTest {
  /** Enable basic tests only environment variable. */
  private val testBasicEnvironment = "TEST_BASIC"

  /** Basic tests enabled only. */
  def testBasic: Boolean =
    Option(System.getenv(testBasicEnvironment)).isDefined
}
