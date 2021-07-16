package automorph.log

/** Compile-time logger used in macros. */
private[automorph] object MacroLogger {

  private val debugProperty = "macro.debug"

  def debug(message: => String): Unit =
    Option(System.getProperty(debugProperty)).foreach(_ => println(message))
}
