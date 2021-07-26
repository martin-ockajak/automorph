package automorph.openapi

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
    Specification(info = info, servers = servers)
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
    Specification(info = info, servers = servers)
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

  private def createServers(serverUrls: Seq[String]): Option[List[Server]] = serverUrls match {
    case Seq() => None
    case Seq(urls: _*) => Some(urls.map(url => Server(url = url)).toList.toList)
  }
}
