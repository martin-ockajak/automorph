package automorph.spi.protocol

/**
 * Renote function descriptor.
 *
 * @param name name
 * @param resultType result type
 * @param parameters parameters
 * @param typeParameters type parameters
 * @param documentation documentation (Scaladoc)
 */
final case class RpcFunction(
  name: String,
  resultType: String,
  parameters: Seq[RpcParameter],
  typeParameters: Seq[RpcParameter],
  documentation: Option[String]
) {

  /** Method signature. */
  lazy val signature: String = {
    val typeParametersText = typeParameters.map { typeParameter =>
      s"${typeParameter.name}"
    } match {
      case Seq()  => ""
      case values => s"[${values.mkString(", ")}]"
    }
    val parametersText = s"(${parameters.map { parameter =>
      s"${parameter.name}: ${parameter.dataType}"
    }.mkString(", ")})"
    s"$name$typeParametersText$parametersText: $resultType"
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
