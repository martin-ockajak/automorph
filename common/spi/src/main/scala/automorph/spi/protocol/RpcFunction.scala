package automorph.spi.protocol

/**
 * Remote function descriptor.
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
      s"${parameter.name}: ${parameter.`type`}"
    }.mkString(", ")})"
    s"$name$parametersText: $resultType"
  }
}

/**
 * Method parameter descriptor.
 *
 * @param name name
 * @param `type` type
 */
final case class RpcParameter(
  name: String,
  `type`: String
)
