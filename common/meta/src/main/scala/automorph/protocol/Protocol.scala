package automorph.protocol

import automorph.protocol.jsonrpc.ErrorType.InvalidResponseException

case object Protocol {
  /** Invalid request error. */
  final case class InvalidRequestException(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /** Invalid response error. */
  final case class InvalidResponseException(
    message: String,
    cause: Throwable
  ) extends RuntimeException(message, cause)

  /**
   * Return specified mandatory property value or throw an exception if it is missing.
   *
   * @param value property value
   * @param name property name
   * @tparam T property type
   * @return property value
   * @throws InvalidRequestException if the property value is missing
   */
  def fromRequest[T](value: Option[T], name: String): T = value.getOrElse(
    throw InvalidRequestException(s"Missing message property: $name", None.orNull)
  )

  /**
   * Return specified mandatory property value or throw an exception if it is missing.
   *
   * @param value property value
   * @param name property name
   * @tparam T property type
   * @return property value
   * @throws InvalidResponseException if the property value is missing
   */
  def fromResponse[T](value: Option[T], name: String): T = value.getOrElse(
    throw InvalidResponseException(s"Missing message property: $name", None.orNull)
  )


  /**
   * Assemble detailed trace of an exception and its causes.
   *
   * @param throwable exception
   * @param maxCauses maximum number of included exception causes
   * @return error messages
   */
  def trace(throwable: Throwable, maxCauses: Int = 100): Seq[String] =
    LazyList.iterate(Option(throwable))(_.flatMap(error => Option(error.getCause)))
      .takeWhile(_.isDefined).flatten.take(maxCauses).map { throwable =>
      val exceptionName = throwable.getClass.getSimpleName
      val message = Option(throwable.getMessage).getOrElse("")
      s"[$exceptionName] $message"
    }
}
