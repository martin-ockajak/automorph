package jsonrpc.core.log

import org.slf4j.LoggerFactory

/**
 * Scala Logging compatible structured logging using SLF4j Mapped Diagnostic Context.
 *
 * Defines `logger` as a value initialized with an underlying `org.slf4j.Logger`.
 * The logger is named according to the class into which this trait is mixed.
 * Can be used as a drop-in replacement for StrictLogging trait in Scala Logging.
 *
 * Scala Logging documentation: https://github.com/lightbend/scala-logging
 * MDC concept description: http://logback.qos.ch/manual/mdc.html
 */
trait Logging:
  protected val logger = Logger(LoggerFactory.getLogger(getClass))

trait StrictLogging:
  protected val logger = Logger(LoggerFactory.getLogger(getClass))

trait LazyLogging:
  protected lazy val logger = Logger(LoggerFactory.getLogger(getClass))
