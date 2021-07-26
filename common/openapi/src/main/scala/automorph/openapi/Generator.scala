package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.util.Method
import automorph.util.Parameter
import javax.swing.plaf.synth.SynthCheckBoxMenuItemUI

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
case object Generator {

  private val objectType = "object"
  private val contentType = "application/json"
  private val jsonRpcRequestTitle = "Request"
  private val jsonRpcRequestDescription = "JSON-RPC request"
  private val argumentsDescription = "Invoked method argument values by name"
  private val scaladocMarkup = "^[/\\* ]*$".r
  private val optionTypePrefix = s"${classOf[Option[Unit]].getName}"

  /**
   * Generate OpenAPI paths for given API methods.
   *
   * @param methods named API
   * @return OpenAPI paths
   */
  def jsonRpcPaths(methods: Map[String, Method]): Paths = toPaths(methods, true)

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URL
   * @return OpenAPI specification
   */
  def jsonRpcSpec(
    methods: Map[String, Method],
    title: String,
    version: String,
    serverUrls: Seq[String]
  ): Specification = Specification(
    paths = Some(toPaths(methods, true)),
    info = Info(title = title, version = version),
    servers = toServers(serverUrls),
    components = toComponents()
  )

  /**
   * Generate OpenAPI paths for given API methods.
   *
   * @param methods named API methods
   * @return OpenAPI paths
   */
  def restRpcPaths(methods: Map[String, Method]): Paths = toPaths(methods, false)

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URL
   * @return OpenAPI specification
   */
  def restRpcSpec(
    methods: Map[String, Method],
    title: String,
    version: String,
    serverUrls: Seq[String]
  ): Specification = Specification(
    paths = Some(toPaths(methods, false)),
    info = Info(title = title, version = version),
    servers = toServers(serverUrls),
    components = toComponents()
  )

  private def parameterSchemas(method: Method): Map[String, Schema] =
    method.parameters.flatten.map(parameter => parameter.name -> parameterSchema(parameter, method.documentation)).toMap

  private def parameterSchema(parameter: Parameter, scaladoc: Option[String]): Schema = {
    val parameterTag = s"${parameter.name} "
    val description = scaladoc.flatMap { doc =>
      maybe(doc.split('\n').flatMap { line =>
        line.split('@') match {
          case Array(prefix, tag, rest @ _*) if tag.startsWith(parameterTag) => Some((tag +: rest).mkString("@").trim)
          case _ => None
        }
      })
    }.map(_.mkString(" "))
    Schema(Some(parameter.dataType), Some(parameter.name), description)
  }

  private def requiredParameters(method: Method): List[String] =
    method.parameters.flatten.filter(_.dataType.startsWith(optionTypePrefix)).map(_.name).toList

  private def jsonRpcSchema(method: Method): Schema = {
    val properties = Map(
      "jsonrpc" -> Schema(Some("string"), Some("jsonrpc"), Some("Protocol version (must be 2.0)")),
      "method" -> Schema(Some("string"), Some("method"), Some("Invoked method name")),
      "params" -> Schema(
        Some(objectType),
        Some(method.name),
        Some(argumentsDescription),
        maybe(parameterSchemas(method)),
        maybe(requiredParameters(method))
      ),
      "id" -> Schema(
        Some("integer"),
        Some("id"),
        Some("Call identifier, a request without and identifier is considered to be a notification")
      )
    )
    val required = List("jsonrpc", "method", "params")
    Schema(
      Some(objectType),
      Some(jsonRpcRequestTitle),
      Some(jsonRpcRequestDescription),
      maybe(properties),
      maybe(required)
    )
  }

  private def restRpcSchema(method: Method): Schema =
    Schema(Some(objectType), Some(method.name), Some(argumentsDescription), maybe(parameterSchemas(method)))

  private def toServers(serverUrls: Seq[String]): Option[Servers] =
    maybe(serverUrls.map(url => Server(url = url)).toList)

  private def toPaths(methods: Map[String, Method], rpc: Boolean): Paths = methods.map { case (name, method) =>
    val path = s"/${name.replace('.', '/')}"
    val summary = method.documentation.flatMap(_.split('\n').find {
      case scaladocMarkup(_*) => true
      case _ => false
    }.map(_.trim))
    val description = method.documentation
    val schema = if (rpc) jsonRpcSchema(method) else restRpcSchema(method)
    val mediaType = MediaType(schema = Some(schema))
    val requestBody = RequestBody(content = Map(contentType -> mediaType), required = Some(true))
    val operation = Operation(requestBody = Some(requestBody))
    val pathItem = PathItem(post = Some(operation), summary = summary, description = description)
    path -> pathItem
  }

  private def toComponents(): Option[Components] = None

  private def maybe[T <: Iterable[_]](iterable: T): Option[T] = if (iterable.isEmpty) None else Some(iterable)
}
