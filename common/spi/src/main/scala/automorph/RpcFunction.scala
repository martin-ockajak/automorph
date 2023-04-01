package automorph

import automorph.RpcFunction.Parameter

/**
 * Remote function descriptor.
 *
 * @param name
 *   name
 * @param resultType
 *   result type
 * @param parameters
 *   parameters
 * @param documentation
 *   documentation (Scaladoc)
 */
final case class RpcFunction(
  name: String,
  parameters: Seq[Parameter],
  resultType: String,
  documentation: Option[String],
) {

  /** Function signature. */
  lazy val signature: String = {
    val parametersText = s"(${parameters.map(parameter => s"${parameter.name}: ${parameter.`type`}").mkString(", ")})"
    s"$name$parametersText: $resultType"
  }
}

object RpcFunction {

  /**
   * Function parameter descriptor.
   *
   * @param name
   *   name
   * @param `type`
   *   type
   */
  final case class Parameter(name: String, `type`: String)
}
