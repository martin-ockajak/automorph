package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.util.Method

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
case object Generator {

  private val objectType = "object"
  private val contentType = "application/json"
  private val parametersTitle = "Parameters"
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

  private def methodSchema(method: Method): Schema = {
    method.parameters.flatten.map { parameter =>
    }
    Schema(title = Some(parametersTitle), `type` = Some(objectType))
  }

  private def jsonRpcSchema(method: Method): Schema = {
    methodSchema(method)
  }

  private def restRpcSchema(method: Method): Schema = {
    methodSchema(method)
  }

  private def toServers(serverUrls: Seq[String]): Option[Servers] = serverUrls match {
    case Seq() => None
    case someServers => Some(serverUrls.map(url => Server(url = url)).toList)
  }

  private def toPaths(methods: Map[String, Method], rpc: Boolean): Paths = methods.map { case (name, method) =>
    val path = s"/${name.replace('.', '/')}"
    val summary = method.documentation.flatMap { doc =>
      doc.split('\n').find {
        case scaladocMarkup(_*) => true
        case _ => false
      }
    }
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
