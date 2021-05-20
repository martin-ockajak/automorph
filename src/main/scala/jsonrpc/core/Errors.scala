package jsonrpc.core
import jsonrpc.core.ValueOps.*

object Errors:
  private val defaultMaxCauses: Int = 100

  // private extensions to make Throwable more scala-like
  extension( throwable: Throwable)
    private def cause: Option[Throwable] = throwable.getCause.asOptionFromNullable
    private def suppressedCauses: Seq[Throwable] = throwable.getSuppressed.toSeq
    private def message: Option[String] = throwable.getMessage.asOptionFromNullable.map(_.trim).flatMap{
      case "" => None
      case x => Some(x)
    }

  /**
   * Assemble concise error descriptions from specified throwable and specified number of its causes.
   *
   * @param throwable exception
   * @param filter only include throwables satisfying this condition
   * @param maxCauses maximum number of included exception causes
   * @return error messages
   */
  def descriptions(
    throwable: Throwable,
    filter: Throwable => Boolean = _ => true,
    maxCauses: Int = defaultMaxCauses
  ): Seq[String] =
    val errors: Seq[Throwable] =
      LazyList.iterate(throwable.asSome)(_.flatMap(_.cause)).takeWhile(_.isDefined).flatten ++ throwable.suppressedCauses
    val filteredErrors = errors.filter(filter).take(maxCauses)
    filteredErrors.map { throwable =>
      val className = throwable.getClass.getSimpleName
      throwable.message.map { msg =>
        s"$className: $msg"
      }.getOrElse(className)
    }
