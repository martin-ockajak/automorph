package test.base

import java.nio.file.Paths
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AppendedClues, BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.Checkers
import scribe.file.{FileWriter, PathBuilder}
import scribe.format.{
  FormatBlock, FormatterInterpolator, cyan, gray, levelColoredPaddedRight, mdcMultiLine, messages, positionSimple
}
import scribe.output.{LogOutput, TextOutput}
import scribe.writer.ConsoleWriter
import scribe.{Level, LogRecord, Logger}

/**
 * Base structured test.
 *
 * Included functionality:
 *   - optional values retrieval support
 *   - before and after test hooks
 *   - result assertion matchers
 *   - additional test clues
 *   - property-based checks
 *   - managed auto-releasing fixtures
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
  with Fixtures {

  override def beforeAll(): Unit = {
    BaseTest.setupLogger
    super.beforeAll()
  }
}

object BaseTest {
  setupLogger

  private object Time extends FormatBlock {
    import perfolation.*

    override def format(record: LogRecord): LogOutput = {
      val timeStamp = record.timeStamp
      new TextOutput(s"${timeStamp.t.T}:${timeStamp.t.L}")
    }
  }

  /** Configure test logging. */
  private lazy val setupLogger: Unit = configureLogger()

  /** Log level environment variable. */
  private lazy val logLevelEnvironment = "LOG_LEVEL"

  /** Enable basic tests only environment variable. */
  private val testBasicEnvironment = "TEST_BASIC"

  /** Configure logging. */
  def configureLogger(): Unit = {
    System.setProperty("org.jboss.logging.provider", "slf4j")
    val level = Option(System.getenv(logLevelEnvironment)).flatMap(Level.get).getOrElse(Level.Fatal)
    val format =
      formatter"${cyan(Time)} [$levelColoredPaddedRight] (${gray(positionSimple)}): $messages$mdcMultiLine"
    val path = PathBuilder.static(Paths.get("target/test.log"))
    Logger.root.clearHandlers().clearModifiers()
      .withHandler(writer = ConsoleWriter, formatter = format, minimumLevel = Some(level))
      .withHandler(writer = FileWriter(path), formatter = format, minimumLevel = Some(Level.Debug)).replace()
    ()
  }

  /** Basic tests enabled only. */
  final def testBasic: Boolean =
    Option(System.getenv(testBasicEnvironment)).isDefined
}
