package automorph.reflection

import automorph.spi.protocol.{RpcFunction, RpcParameter}

/**
 * Method descriptor.
 *
 * @param name name
 * @param resultType result type
 * @param parameters parameters
 * @param typeParameters type parameters
 * @param public true if this method is publicly accessible
 * @param available true if it possible to call this method
 * @param documentation documentation (Scaladoc)
 */
final private[automorph] case class Method(
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
      case Seq() => ""
      case values => s"[${values.mkString(", ")}]"
    }
    val parametersText = parameters.map { parameters =>
      s"(${parameters.map { parameter =>
        s"${parameter.name}: ${parameter.dataType}"
      }.mkString(", ")})"
    }.mkString
    s"$name$typeParametersText$parametersText: $resultType"
  }

  /** RPC function descriptor. */
  lazy val rpcFunction: RpcFunction = RpcFunction(
    name,
    parameters.flatten.map { case Parameter(name, dataType, _) => RpcParameter(name, dataType) },
    resultType,
    documentation
  )
}

/**
 * Method parameter descriptor.
 *
 * @param name name
 * @param dataType type
 * @param contextual true if this parameter is implicit
 */
final private[automorph] case class Parameter(
  name: String,
  dataType: String,
  contextual: Boolean
)
