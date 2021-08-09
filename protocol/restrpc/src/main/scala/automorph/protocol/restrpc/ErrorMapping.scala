package automorph.protocol.restrpc

private[automorph] trait ErrorMapping {

  /**
   * Maps a REST-RPC error to a corresponding default exception.
   *
   * @param message error message
   * @param code error code
   * @return exception
   */
  def defaultErrorToException(message: String, code: Option[Int]): Throwable = code match {
    case _ => new RuntimeException(message)
  }

  /**
   * Maps an exception to a corresponding default REST-RPC error type.
   *
   * @param exception exception
   * @return REST-RPC error type
   */
  def defaultExceptionToError(exception: Throwable): Option[Int] = exception match {
    case _ => None
  }
}
