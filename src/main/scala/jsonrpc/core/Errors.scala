package jsonrpc.core

import jsonrpc.util.ValueOps.{asOption, asSome}

case object Errors:

  // private extensions to make Throwable more scala-like
  extension (throwable: Throwable)
    private def cause: Option[Throwable] = throwable.getCause.asOption
    private def message: Option[String] = throwable.getMessage.asOption.map(_.trim).filter(_.nonEmpty)

  /**
   * Assemble concise error descriptions from specified throwable and specified number of its filtered causes.
   *
   * @param throwable exception
   * @param filter only include throwables satisfying this condition
   * @param maxCauses maximum number of included exception causes
   * @return error messages
   */
  def descriptions(
    throwable: Throwable,
    filter: Throwable => Boolean = _ => true,
    maxCauses: Int = 100
  ): Seq[String] =
    LazyList.iterate(throwable.asSome)(_.flatMap(_.cause))
      .takeWhile(_.isDefined).flatten.filter(filter).take(maxCauses).map { throwable =>
        val exceptionName = throwable.getClass.getSimpleName
        throwable.message.map { message =>
          s"$exceptionName: $message"
        }.getOrElse(exceptionName)
      }
