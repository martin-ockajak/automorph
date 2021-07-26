package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.util.Method
import javax.swing.plaf.synth.SynthCheckBoxMenuItemUI

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
case object Generator {

  private val objectType = "object"
  private val contentType = "application/json"
  private val jsonRpcRequestTitle = "JSON-RPC request"
  private val scaladocMarkup = "^[/\\* ]*$".r

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

  private def toParametersSchema(method: Method): Schema = {
    val properties = method.parameters.flatten.map { parameter =>
      val parameterTag = s"${method.name} "
      val description = method.documentation.flatMap(_.split('\n').flatMap { line =>
        line.split('@') match {
          case Array(prefix, tag, rest @ _*) if tag.startsWith(parameterTag) => Some((tag +: rest).mkString("@").trim)
          case _ => None
        }
      } match {
        case Array() => None
        case lines => Some(lines.mkString(" "))
      })
      parameter.name -> Property(`type` = parameter.dataType, description = description)
    }.toMap
    Schema(title = Some(method.name), `type` = Some(objectType), properties = Some(properties))
  }

  private def jsonRpcSchema(method: Method): Schema = {
    val parametersSchema = toParametersSchema(method)
    val properties = Map(
      "jsonrpc" -> Property("string", None, Some("jsonrpc"), Some("Protocol version (must be 2.0)")),
      "method" -> Property("string", None, Some("method"), Some("Invoked method name")),
      "params" -> Property("object", None, Some("params"), Some("invoked method argument values position by name")),
      "id" -> Property("integer", None, Some("id"), Some("Call identifier, a request without and identifier is considered to be a notification")),
    )
    val required = List("jsonrpc", "method", "params")
    Schema(Some(jsonRpcRequestTitle), `type` = Some(objectType), properties = Some(properties), required = Some(required))
  }

  private def restRpcSchema(method: Method): Schema = toParametersSchema(method)

  private def toServers(serverUrls: Seq[String]): Option[Servers] = serverUrls match {
    case Seq() => None
    case someServers => Some(serverUrls.map(url => Server(url = url)).toList)
  }

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
}
