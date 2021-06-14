package jsonrpc.log

import org.slf4j
import org.slf4j.{LoggerFactory, MDC}

/**
 * Scala Logging compatible structured logger implicit SLF4J Mapped Diagnostic Context.
 *
 * Can be used as a drop-in replacement for Logger class in Scala Logging.
 *
 * @see [[https://github.com/lightbend/scala-logging Scala Logging documentation]]
 * @see [[http://logback.qos.ch/manual/mdc.html MDC concept description]]
 * @param underlying underlying [[https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.30/org/slf4j/Logger.html SLF4J logger]]
 */
@SerialVersionUID(782158461L)
final case class Logger private (private val underlying: slf4j.Logger) {

  type Not[T] = T => Nothing
  type Or[T, U] = Not[Not[T] with Not[U]]

  def error[T](message: => String): Unit = underlying.error(message)

  def error[T](message: => String, cause: => Throwable): Unit = underlying.error(message, cause)

  def error[T](message: => String, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit = log(message, properties, underlying.isErrorEnabled, message => underlying.error(message))

  def error[T](message: => String, cause: => Throwable, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit =
    log(message, cause, properties, underlying.isErrorEnabled, (message, cause) => underlying.error(message, cause))

  def error[T](message: => String, properties: (String, Any)*): Unit = error(message, properties)

  def error[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    error(message, cause, properties)

  def warn[T](message: => String): Unit = underlying.warn(message)

  def warn[T](message: => String, cause: => Throwable): Unit = underlying.warn(message, cause)

  def warn[T](message: => String, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit = log(message, properties, underlying.isWarnEnabled, message => underlying.warn(message))

  def warn[T](message: => String, cause: => Throwable, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit =
    log(message, cause, properties, underlying.isWarnEnabled, (message, cause) => underlying.warn(message, cause))

  def warn[T](message: => String, properties: (String, Any)*): Unit = warn(message, properties)

  def warn[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    warn(message, cause, properties)

  def info[T](message: => String): Unit = underlying.info(message)

  def info[T](message: => String, cause: => Throwable): Unit = underlying.info(message, cause)

  def info[T](message: => String, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit = log(message, properties, underlying.isInfoEnabled, message => underlying.info(message))

  def info[T](message: => String, cause: => Throwable, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit =
    log(message, cause, properties, underlying.isInfoEnabled, (message, cause) => underlying.info(message, cause))

  def info[T](message: => String, properties: (String, Any)*): Unit = info(message, properties)

  def info[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    info(message, cause, properties)

  def debug[T](message: => String): Unit = underlying.debug(message)

  def debug[T](message: => String, cause: => Throwable): Unit = underlying.debug(message, cause)

  def debug[T](message: => String, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit = log(message, properties, underlying.isDebugEnabled, message => underlying.debug(message))

  def debug[T](message: => String, cause: => Throwable, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit =
    log(message, cause, properties, underlying.isDebugEnabled, (message, cause) => underlying.debug(message, cause))

  def debug[T](message: => String, properties: (String, Any)*): Unit = debug(message, properties)

  def debug[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    debug(message, cause, properties)

  def trace[T](message: => String): Unit = underlying.trace(message)

  def trace[T](message: => String, cause: => Throwable): Unit = underlying.trace(message, cause)

  def trace[T](message: => String, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit = log(message, properties, underlying.isTraceEnabled, message => underlying.trace(message))

  def trace[T](message: => String, cause: => Throwable, properties: => T)(implicit
    evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit =
    log(message, cause, properties, underlying.isTraceEnabled, (message, cause) => underlying.trace(message, cause))

  def trace[T](message: => String, properties: (String, Any)*): Unit = trace(message, properties)

  def trace[T](message: => String, cause: => Throwable, properties: (String, Any)*): Unit =
    trace(message, cause, properties)

  private def log[T](message: => String, properties: => T, enabled: Boolean, logMessage: String => Unit)(
    implicit evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])
  ): Unit =
    if (enabled) {
      val iterableProperties = unpackProperties(properties)
      addDiagnosticContext(iterableProperties)
      logMessage(s"$message\n${formatProperties(iterableProperties)}\n")
      removeDiagnosticContext(iterableProperties)
    }

  private def log[T](
    message: => String,
    cause: => Throwable,
    properties: => T,
    enabled: Boolean,
    logMessage: (String, Throwable) => Unit
  )(implicit evidence: Not[Not[T]] <:< (Or[Iterable[(String, Any)], Product])): Unit =
    if (enabled) {
      val iterableProperties = unpackProperties(properties)
      addDiagnosticContext(iterableProperties)
      logMessage(s"$message\n${formatProperties(iterableProperties)}\n", cause)
      removeDiagnosticContext(iterableProperties)
    }

  private def unpackProperties[T](properties: => T): Iterable[(String, Any)] =
    properties match {
      case product: Product      => productProperties(product)
      case iterable: Iterable[_] => iterable.asInstanceOf[Iterable[(String, Any)]]
    }

  private def productProperties(product: Product): Map[String, Any] =
    product.productElementNames.map(_.capitalize).zip(product.productIterator).toMap

  private def formatProperties(properties: Iterable[(String, Any)]): String =
    properties.map { case (key, value) => s"$key = ${format(value)}" }.mkString("\n")

  private def addDiagnosticContext(properties: Iterable[(String, Any)]): Unit =
    properties.foreach { case (key, value) => MDC.put(key, format(value)) }

  private def removeDiagnosticContext(properties: Iterable[(String, Any)]): Unit =
    properties.foreach { case (key, _) => MDC.remove(key) }

  private def format(value: Any): String = value match {
    case stringValue: String => stringValue
    case anyValue            => Logger.prettyPrint(anyValue).plainText
  }
}

case object Logger {

  private val prettyPrint = pprint.PPrinter.BlackWhite.copy(defaultIndent = 2, defaultWidth = 0)

  /**
   * Create a [[Logger]] implicit the underlying `org.slf4j.Logger`.
   *
   * @param underlying underlying [[https://www.javadoc.io/doc/org.slf4j/slf4j-api/1.7.30/org/slf4j/Logger.html SLF4J logger]]
   * @return logger
   */
  def apply(underlying: slf4j.Logger): Logger = new Logger(underlying)

  /**
   * Create a [[Logger]] with the specified name.
   *
   * @param name logger name
   * @return logger
   */
  def apply(name: String): Logger = new Logger(LoggerFactory.getLogger(name))
}
