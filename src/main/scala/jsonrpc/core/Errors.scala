package jsonrpc.core

object Errors:
  val defaultMaxCauses: Int = 100
  val messageDelimiter = ":"

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
    val errors = LazyList.iterate(Option(throwable))(_.flatMap { throwable =>
      Option(throwable.getCause)
    }).takeWhile(_.isDefined).flatten ++ throwable.getSuppressed
    val filteredErrors = errors.filter(filter).take(maxCauses)
    filteredErrors.map { throwable =>
      val className = throwable.getClass.getSimpleName
      Option(throwable.getMessage).map { message =>
        s"$className$messageDelimiter $message"
      }.getOrElse(className)
    }
