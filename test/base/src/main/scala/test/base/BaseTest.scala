package test.base

import java.nio.file.Paths
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AppendedClues, BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.Checkers
import scribe.Level
import scribe.file.{FileWriter, PathBuilder}
import scribe.format.{Formatter, FormatterInterpolator, gray, levelColoredPaddedRight, mdcMultiLine, message, positionAbbreviated, time}
import scribe.writer.ConsoleWriter

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
  with Network {

  override def beforeAll(): Unit =
    BaseTest.setupLogger()
}

object BaseTest {

  /** Log level environment variable. */
  private val logLevelEnvironment = "LOG_LEVEL"

  /** Enable basic tests only environment variable. */
  private val testBasicEnvironment = "TEST_BASIC"

  /** Basic tests enabled only. */
  final def testBasic: Boolean =
    Option(System.getenv(testBasicEnvironment)).isDefined

  private def setupLogger(): Unit = {
    val level = Option(System.getenv(logLevelEnvironment)).flatMap(Level.get).getOrElse(Level.Info)
    val format = formatter"$time [$levelColoredPaddedRight] (${gray(positionAbbreviated)}): $message$mdcMultiLine"
//    val path = PathBuilder.static(Paths.get("target/test.log"))
    scribe.Logger.root
      .clearHandlers()
      .clearModifiers()
      .withHandler(writer = ConsoleWriter, formatter = format, minimumLevel = Some(level))
//      .withHandler(writer = FileWriter(path), formatter = format, minimumLevel = Some(level))
      .replace()
  }
}
