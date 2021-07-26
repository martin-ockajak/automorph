package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.util.Method

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
case object Generator {

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
  ): Specification =
    Specification(
      paths = Some(toPaths(methods, true)),
      info = Info(title = title, version = version),
      servers = toServers(serverUrls)
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
  ): Specification =
    Specification(
      paths = Some(toPaths(methods, false)),
      info = Info(title = title, version = version),
      servers = toServers(serverUrls)
    )

  private def toServers(serverUrls: Seq[String]): Option[Servers] = serverUrls match {
    case Seq() => None
    case someServers => Some(serverUrls.map(url => Server(url = url)).toList)
  }

  private def toPaths(methods: Map[String, Method], rpc: Boolean): Paths = methods.map { case (name, method) =>
    val path = s"/${name.replace('.', '/')}"
    val pathItem = if (rpc) jsonRpcPathItem(method) else restRpcPathItem(method)
    path -> pathItem
  }

  private def jsonRpcPathItem(method: Method): PathItem =
    PathItem(summary = toSummary(method.documentation), description = method.documentation)

  private def restRpcPathItem(method: Method): PathItem =
    PathItem(summary = toSummary(method.documentation), description = method.documentation)

  private def toSummary(scaladoc: Option[String]): Option[String] = scaladoc.flatMap { doc =>
    doc.split('\n').find {
      case scaladocMarkup(_*) => true
      case _ => false
    }
  }

  private def toComponents(): Option[Components] = None
}
