package automorph.util

/**
 * Method descriptor.
 *
 * @param name name
 * @param resultType result type
 * @param parameters parameters
 * @param typeParameters type parameters
 * @param public true if the method is publicly accessible
 * @param available true if the method can be called
 * @param documentation documentation
 */
final case class Method(
  name: String,
  resultType: String,
  parameters: Seq[Seq[Parameter]],
  typeParameters: Seq[Parameter],
  public: Boolean,
  available: Boolean,
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
    val parametersText = parameters.map { parameters =>
      s"(${parameters.map { parameter =>
        s"${parameter.name}: ${parameter.dataType}"
      }.mkString(", ")})"
    }.mkString
    s"$name$typeParametersText$parametersText: $resultType"
  }
}

/**
 * Method parameter descriptor.
 *
 * @param name name
 * @param dataType type
 * @param contextual true if this parameter is implicit
 */
final case class Parameter(
  name: String,
  dataType: String,
  contextual: Boolean
)
