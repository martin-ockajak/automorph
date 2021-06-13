package jsonrpc.log

import java.nio.file.{Files, Path, Paths}
import org.slf4j.{LoggerFactory, MDC, Logger as Underlying}

/**
 * Scala Logging compatible structured logger using SLF4J Mapped Diagnostic Context.
 *
 * Can be used as a drop-in replacement for Logger class in Scala Logging.
 *
 * @see [[https://github.com/lightbend/scala-logging Scala Logging documentation]]
 * @see [[http://logback.qos.ch/manual/mdc.html MDC concept description]]
 * @param underlying underlying [[https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.30/org/slf4j/Logger.html SLF4J logger]]
 */
@SerialVersionUID(782158461L)
final case class Logger private (private val underlying: Underlying) {

  type Not[T] = T => Nothing
  infix type Or[T, U] = Not[Not[T] & Not[U]]

  def error[T](message: => String): Unit = underlying.error(message)

  def error[T](message: => String, cause: => Throwable): Unit = underlying.error(message, cause)

  def error[T <: Matchable](message: => String, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, properties, underlying.isErrorEnabled, message => underlying.error(message))

  def error[T <: Matchable](message: => String, cause: => Throwable, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, cause, properties, underlying.isErrorEnabled, (message, cause) => underlying.error(message, cause))

  def error[T](message: => String, properties: (String, Any)*): Unit = error(message, properties)

  def error[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    error(message, cause, properties)

  def warn[T](message: => String): Unit = underlying.warn(message)

  def warn[T](message: => String, cause: => Throwable): Unit = underlying.warn(message, cause)

  def warn[T <: Matchable](message: => String, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, properties, underlying.isWarnEnabled, message => underlying.warn(message))

  def warn[T <: Matchable](message: => String, cause: => Throwable, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, cause, properties, underlying.isWarnEnabled, (message, cause) => underlying.warn(message, cause))

  def warn[T](message: => String, properties: (String, Any)*): Unit = warn(message, properties)

  def warn[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    warn(message, cause, properties)

  def info[T](message: => String): Unit = underlying.info(message)

  def info[T](message: => String, cause: => Throwable): Unit = underlying.info(message, cause)

  def info[T <: Matchable](message: => String, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, properties, underlying.isInfoEnabled, message => underlying.info(message))

  def info[T <: Matchable](message: => String, cause: => Throwable, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, cause, properties, underlying.isInfoEnabled, (message, cause) => underlying.info(message, cause))

  def info[T](message: => String, properties: (String, Any)*): Unit = info(message, properties)

  def info[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    info(message, cause, properties)

  def debug[T](message: => String): Unit = underlying.debug(message)

  def debug[T](message: => String, cause: => Throwable): Unit = underlying.debug(message, cause)

  def debug[T <: Matchable](message: => String, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, properties, underlying.isDebugEnabled, message => underlying.debug(message))

  def debug[T <: Matchable](message: => String, cause: => Throwable, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, cause, properties, underlying.isDebugEnabled, (message, cause) => underlying.debug(message, cause))

  def debug[T](message: => String, properties: (String, Any)*): Unit = debug(message, properties)

  def debug[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    debug(message, cause, properties)

  def trace[T](message: => String): Unit = underlying.trace(message)

  def trace[T](message: => String, cause: => Throwable): Unit = underlying.trace(message, cause)

  def trace[T <: Matchable](message: => String, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, properties, underlying.isTraceEnabled, message => underlying.trace(message))

  def trace[T <: Matchable](message: => String, cause: => Throwable, properties: => T)(using
    evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit =
    log(message, cause, properties, underlying.isTraceEnabled, (message, cause) => underlying.trace(message, cause))

  def trace[T](message: => String, properties: (String, Any)*): Unit = trace(message, properties)

  def trace[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    trace(message, cause, properties)

  private def log[T <: Matchable](message: => String, properties: => T, enabled: Boolean, logMessage: String => Unit)(
    using evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)
  ): Unit = {
    if (enabled) {
      val iterableProperties = unpackProperties(properties)
      addDiagnosticContext(iterableProperties)
      logMessage(s"$message\n${formatProperties(iterableProperties)}\n")
      removeDiagnosticContext(iterableProperties)
    }
  }

  private def log[T <: Matchable](
    message: => String,
    cause: => Throwable,
    properties: => T,
    enabled: Boolean,
    logMessage: (String, Throwable) => Unit
  )(using evidence: Not[Not[T]] <:< (Iterable[(String, Any)] Or Product)): Unit =
    if enabled then
      val iterableProperties = unpackProperties(properties)
      addDiagnosticContext(iterableProperties)
      logMessage(s"$message\n${formatProperties(iterableProperties)}\n", cause)
      removeDiagnosticContext(iterableProperties)

  private def unpackProperties[T <: Matchable](properties: => T): Iterable[(String, Matchable)] =
    val iterableProperties =
      properties match
        case product: Product      => productProperties(product)
        case iterable: Iterable[?] => iterable
    // FIXME - find a way to avoid Matchable type coercion
    iterableProperties.asInstanceOf[Iterable[(String, Matchable)]]

  private def productProperties(product: Product): Map[String, Any] =
    product.productElementNames.map(_.capitalize).zip(product.productIterator).toMap

  private def formatProperties(properties: Iterable[(String, Matchable)]): String =
    properties.map { case (key, value) => s"$key = ${format(value)}" }.mkString("\n")

  private def addDiagnosticContext(properties: Iterable[(String, Matchable)]): Unit =
    properties.foreach { case (key, value) => MDC.put(key, format(value)) }

  private def removeDiagnosticContext(properties: Iterable[(String, Any)]): Unit =
    properties.foreach { case (key, _) => MDC.remove(key) }

  private def format(value: Matchable): String =
    value match
      case stringValue: String => stringValue
      case anyValue            => Logger.prettyPrint(anyValue).plainText
}

case object Logger {

  private val prettyPrint = pprint.PPrinter.BlackWhite.copy(defaultIndent = 2, defaultWidth = 0)

  /**
   * Create a [[Logger]] using the underlying `org.slf4j.Logger`.
   *
   * @param underlying underlying [[https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.30/org/slf4j/Logger.html SLF4J logger]]
   * @return logger
   */
  def apply(underlying: Underlying): Logger = new Logger(underlying)

  /**
   * Create a [[Logger]] with the specified name.
   *
   * @param name logger name
   * @return logger
   */
  def apply(name: String): Logger = new Logger(LoggerFactory.getLogger(name))
}
