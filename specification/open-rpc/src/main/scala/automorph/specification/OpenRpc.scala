package automorph.specification

import automorph.specification.jsonschema.Schema
import automorph.specification.openrpc.{Components, ContentDescriptor, ExternalDocumentation, Info, Method, Server}
import automorph.spi.protocol.RpcFunction

/**
 * OpenRPC API description.
 *
 * @see [[https://spec.open-rpc.org OpenRPC description]]
 */
case class OpenRpc(
  openrpc: String = "1.2.6",
  info: Info,
  servers: Option[List[Server]] = None,
  methods: List[Method] = List(),
  components: Option[Components] = None,
  externalDocs: Option[ExternalDocumentation] = None
)

object OpenRpc {

  /** Result value name. */
  val resultName = "result"

  private val defaultTitle = ""
  private val defaultVersion = ""
  private val scaladocMarkup = "^[/\\* ]*$".r

  /**
   * Generates OpenRPC description for given RPC functions.
   *
   * @param functions RPC functions
   * @return OpenRPC description
   */
  def apply(functions: Iterable[RpcFunction]): OpenRpc = {
    val methods = functions.map { function =>
      // Parameters
      val parameterSchemas = Schema.parameters(function)
      val requiredParameters = Schema.requiredParameters(function).toSet
      val params = function.parameters.map { parameter =>
        ContentDescriptor(
          name = parameter.name,
          summary = Some(parameter.`type`),
          required = Some(requiredParameters.contains(parameter.name)),
          schema = parameterSchemas(parameter.name)
        )
      }.toList

      // Result
      val result = ContentDescriptor(
        name = resultName,
        summary = Some(function.resultType),
        required = Some(true),
        schema = Schema.result(function)
      )

      // Method
      val summary = function.documentation.flatMap(_.split('\n').find {
        case scaladocMarkup(_*) => true
        case _ => false
      }.map(_.trim))
      val method = Method(
        name = function.name,
        params = params,
        result = result,
        summary = summary,
        description = function.documentation,
        paramStructure = Some("either")
      )
      method
    }.toList
    val info = Info(title = defaultTitle, version = defaultVersion)
    OpenRpc(info = info, methods = methods)
  }
}
