package jsonrpc.util

final case class Method(
  name: String,
  resultType: String,
  parameters: Seq[Seq[Parameter]],
  typeParameters: Seq[TypeParameter],
  public: Boolean,
  available: Boolean,
  documentation: Option[String]
) {

  /** Method signature. */
  lazy val signature: String = {
    val typeParametersText = typeParameters.map { typeParameter =>
      s"${typeParameter.name}"
    } match {
      case Seq() => ""
      case values => s"[${values.mkString(", ")}]"
    }
    val parametersText = parameters.map { parameters =>
      s"(${
        parameters.map { parameter =>
          s"${parameter.name}: ${parameter.dataType}"
        }.mkString(", ")
      })"
    }.mkString
    s"$name$typeParametersText$parametersText: $resultType"
  }
}

final case class Parameter(
  name: String,
  dataType: String,
  contextual: Boolean
)

final case class TypeParameter(
  name: String,
  bounds: String
)
