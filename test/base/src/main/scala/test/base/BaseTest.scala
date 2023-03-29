package test.base

import java.nio.file.Paths
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AppendedClues, BeforeAndAfterAll, BeforeAndAfterEach, OptionValues}
import org.scalatestplus.scalacheck.Checkers
import scribe.file.{FileWriter, PathBuilder}
import scribe.format.{
  FormatterInterpolator, dateFull, gray, levelColoredPaddedRight, magenta, mdcMultiLine, messages, positionSimple,
}
import scribe.writer.ConsoleWriter
import scribe.{Level, Logger}

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
    override def format(record: LogRecord): LogOutput = {
      val timesStamp = record.timeStamp
      new TextOutput(s"${timesStamp.t.T}:${timesStamp.t.L}")
    }
  }

  /** Configure test logging. */
  private lazy val setupLogger: Unit = {
    System.setProperty("org.jboss.logging.provider", "slf4j")
    val level = Option(System.getenv(logLevelEnvironment)).flatMap(Level.get).getOrElse(Level.Fatal)
    val format =
      formatter"${magenta(Time)} [$levelColoredPaddedRight] (${gray(positionSimple)}): $messages$mdcMultiLine"
    val path = PathBuilder.static(Paths.get("target/test.log"))
    Logger.root.clearHandlers().clearModifiers()
      .withHandler(writer = ConsoleWriter, formatter = format, minimumLevel = Some(level))
      .withHandler(writer = FileWriter(path), formatter = format, minimumLevel = Some(Level.Debug)).replace()
    ()
  }

  /** Log level environment variable. */
  private lazy val logLevelEnvironment = "LOG_LEVEL"

  /** Enable basic tests only environment variable. */
  private val testBasicEnvironment = "TEST_BASIC"

  /** Basic tests enabled only. */
  final def testBasic: Boolean =
    Option(System.getenv(testBasicEnvironment)).isDefined
}
