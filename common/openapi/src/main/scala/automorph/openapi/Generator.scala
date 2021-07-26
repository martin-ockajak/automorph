package automorph.openapi

import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.util.Method

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
case object Generator {

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param info OpenAPI Info object
   * @param serverUrls server URLs
   * @return OpenAPI specification
   */
  def jsonRpc(methods: Map[String, Method], info: Info, serverUrls: Seq[String] = Seq()): Specification = {
    val servers = createServers(serverUrls)
    val paths = createJsonRpcPaths(methods)
    val components = createComponents()
    Specification(info = info, servers = servers, paths = paths, components = components)
  }

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @return OpenAPI specification
   */
  def jsonRpc(
    methods: Map[String, Method],
    title: String,
    version: String
  ): Specification = jsonRpc(methods, Info(title = title, version = version))

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param info OpenAPI Info object
   * @param serverUrls server URLs
   * @return OpenAPI specification
   */
  def restRpc(methods: Map[String, Method], info: Info, serverUrls: Seq[String] = Seq()): Specification = {
    val servers = createServers(serverUrls)
    val paths = createJsonRpcPaths(methods)
    val components = createComponents()
    Specification(info = info, servers = servers, paths = paths, components = components)
  }

  /**
   * Generate OpenAPI specification for given API methods.
   *
   * @param methods named API methods
   * @param title API title
   * @param version API specification version
   * @return OpenAPI specification
   */
  def restRpc(
    methods: Map[String, Method],
    title: String,
    version: String
  ): Specification = restRpc(methods, Info(title = title, version = version))

  private def createServers(serverUrls: Seq[String]): Option[Servers] = serverUrls match {
    case Seq() => None
    case Seq(urls: _*) => Some(urls.map(url => Server(url = url)).toList.toList)
  }

  private def createJsonRpcPaths(methods: Map[String, Method]): Option[Paths] =
    None

  private def createRestRpcPaths(methods: Map[String, Method]): Option[Paths] =
    None

  private def createComponents(): Option[Components] = None
}
