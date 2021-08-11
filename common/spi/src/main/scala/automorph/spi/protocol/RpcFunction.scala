package automorph.spi.protocol

/**
 * Renote function descriptor.
 *
 * @param name name
 * @param resultType result type
 * @param parameters parameters
 * @param documentation documentation (Scaladoc)
 */
final case class RpcFunction(
  name: String,
  parameters: Seq[RpcParameter],
  resultType: String,
  documentation: Option[String]
) {

  /** Method signature. */
  lazy val signature: String = {
    val parametersText = s"(${parameters.map { parameter =>
      s"${parameter.name}: ${parameter.dataType}"
    }.mkString(", ")})"
    s"$name$parametersText: $resultType"
  }
}

/**
 * Method parameter descriptor.
 *
 * @param name name
 * @param dataType type
 */
final case class RpcParameter(
  name: String,
  dataType: String
)
