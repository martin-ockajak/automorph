package automorph.protocol.restrpc

private[automorph] trait ErrorMapping {

  /**
   * Maps a Web-RPC error to a corresponding default exception.
   *
   * @param message error message
   * @param code error code
   * @return exception
   */
  def defaultMapError(message: String, code: Option[Int]): Throwable = code match {
    case _ => new RuntimeException(message)
  }

  /**
   * Maps an exception to a corresponding default Web-RPC error type.
   *
   * @param exception exception
   * @return Web-RPC error type
   */
  def defaultMapException(exception: Throwable): Option[Int] = exception match {
    case _ => None
  }
}
