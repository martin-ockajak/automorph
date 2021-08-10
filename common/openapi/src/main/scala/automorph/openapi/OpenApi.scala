package automorph.openapi

import automorph.openapi.Schema.Properties
import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.spi.protocol.RpcFunction

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
object OpenApi {

  private val contentType = "application/json"
  private val objectType = "object"
  private val httpStatusCodeOk = 200.toString
  private val requestTitle = "Request"
  private val resultTitle = "Result"
  private val errorTitle = "Error"
  private val jsonRpcPrefix = "JSON-RPC "
  private val restRpcPrefix = "REST-RPC "
  private val argumentsDescription = "Function argument values by name"
  private val scaladocMarkup = "^[/\\* ]*$".r
  private val optionTypePrefix = s"${classOf[Option[Unit]].getName}"
//  implicit private val pathsEncoder: Encoder[Paths] = new Encoder[Paths] {
//    def apply(v: Paths): Json = Json.obj((v.map { case (key, value) =>
//      key -> value.asJson
//    }.toSeq)*)
//  }
//  implicit private val pathsEncoder: Encoder[Paths] = Encoder.encodeMap
//  implicit private val pathsDecoder: Decoder[Paths] = Decoder.decodeMap
//  implicit private val componentsEncoder: Encoder[Components] = Encoder.encodeMap
//  implicit private val componentsDecoder: Decoder[Components] = Decoder.decodeMap
//    implicit private val propertiesEncoder: Encoder[Properties] = Encoder.encodeMap
//    implicit private val propertiesDecoder: Decoder[Properties] = Decoder.decodeMap

  /**
   * Generate OpenAPI paths for specified API functions.
   *
   * @param functions API functions
   * @return OpenAPI paths
   */
  def jsonRpcPaths(functions: Iterable[RpcFunction]): Paths = toPaths(functions.map { function =>
    function -> RpcSchema(jsonRpcRequestSchema(function), jsonRpcResultSchema(function), jsonRpcErrorSchema)
  })

  /**
   * Generate OpenAPI paths for specified API functions.
   *
   * @param functions API functions
   * @return OpenAPI paths
   */
  def restRpcPaths(functions: Iterable[RpcFunction]): Paths = toPaths(functions.map { function =>
    function -> RpcSchema(restRpcRequestSchema(function), restRpcResultSchema(function), restRpcErrorSchema)
  })

  /**
   * Generate OpenAPI specification for specified API functions.
   *
   * @param functions API functions
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URLs
   * @return OpenAPI specification
   */
  def jsonRpcSpec(
    functions: Iterable[RpcFunction],
    title: String,
    version: String,
    serverUrls: Seq[String]
  ): Specification = Specification(
    paths = Some(jsonRpcPaths(functions)),
    info = Info(title = title, version = version),
    servers = toServers(serverUrls),
    components = toComponents()
  )

  /**
   * Generate OpenAPI specification for specified API functions.
   *
   * @param functions API functions
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URLs
   * @return OpenAPI specification
   */
  def restRpcSpec(
    functions: Iterable[RpcFunction],
    title: String,
    version: String,
    serverUrls: Seq[String]
  ): Specification = Specification(
    paths = Some(restRpcPaths(functions)),
    info = Info(title = title, version = version),
    servers = toServers(serverUrls),
    components = toComponents()
  )

  private def parameterSchemas(function: RpcFunction): Map[String, Schema] =
    function.parameters.map { parameter =>
      // FIXME - convert data type to JSON type
      parameter.name -> Schema(
        Some(parameter.dataType),
        Some(parameter.name),
        scaladocField(function.documentation, s"param ${parameter.name}")
      )
    }.toMap

  private def resultSchema(function: RpcFunction): Schema =
    // FIXME - convert data type to JSON type
    Schema(Some(function.resultType), Some("result"), scaladocField(function.documentation, "return"))

  private def requiredParameters(function: RpcFunction): List[String] =
    function.parameters.filter(_.dataType.startsWith(optionTypePrefix)).map(_.name).toList

  private def jsonRpcRequestSchema(function: RpcFunction): Schema = Schema(
    Some(objectType),
    Some(requestTitle),
    Some(s"$jsonRpcPrefix$requestTitle"),
    Some(Map(
      "jsonrpc" -> Schema(Some("string"), Some("jsonrpc"), Some("Protocol version (must be 2.0)")),
      "function" -> Schema(Some("string"), Some("function"), Some("Invoked function name")),
      "params" -> Schema(
        Some(objectType),
        Some(function.name),
        Some(argumentsDescription),
        maybe(parameterSchemas(function)),
        maybe(requiredParameters(function))
      ),
      "id" -> Schema(
        Some("integer"),
        Some("id"),
        Some("Call identifier, a request without and identifier is considered to be a notification")
      )
    )),
    Some(List("jsonrpc", "function", "params"))
  )

  private def restRpcRequestSchema(function: RpcFunction): Schema =
    Schema(Some(objectType), Some(function.name), Some(argumentsDescription), maybe(parameterSchemas(function)))

  private def jsonRpcResultSchema(function: RpcFunction): Schema = Schema(
    Some(objectType),
    Some(resultTitle),
    Some(s"$jsonRpcPrefix$resultTitle"),
    Some(Map("result" -> resultSchema(function))),
    Some(List("result"))
  )

  private def restRpcResultSchema(function: RpcFunction): Schema = Schema(
    Some(objectType),
    Some(resultTitle),
    Some(s"$restRpcPrefix$resultTitle"),
    Some(Map("result" -> resultSchema(function))),
    Some(List("result"))
  )

  private def jsonRpcErrorSchema: Schema = Schema(
    Some(objectType),
    Some(errorTitle),
    Some(s"$jsonRpcPrefix$errorTitle"),
    Some(Map(
      "error" -> Schema(
        Some("string"),
        Some("error"),
        Some("Failed function call error details"),
        Some(Map(
          "code" -> Schema(Some("integer"), Some("code"), Some("Error code")),
          "message" -> Schema(Some("string"), Some("message"), Some("Error message")),
          "data" -> Schema(Some("object"), Some("data"), Some("Additional error information"))
        )),
        Some(List("code", "message"))
      )
    )),
    Some(List("error"))
  )

  private def restRpcErrorSchema: Schema = Schema(
    Some(objectType),
    Some(errorTitle),
    Some(s"$restRpcPrefix$errorTitle"),
    Some(Map(
      "error" -> Schema(
        Some("string"),
        Some("error"),
        Some("Failed function call error details"),
        Some(Map(
          "code" -> Schema(Some("integer"), Some("code"), Some("Error code")),
          "message" -> Schema(Some("string"), Some("message"), Some("Error message")),
          "data" -> Schema(Some("object"), Some("data"), Some("Additional error information"))
        )),
        Some(List("code", "message"))
      )
    )),
    Some(List("error"))
  )

  private def scaladocField(scaladoc: Option[String], field: String): Option[String] = {
    val fieldPrefix = s"$field "
    scaladoc.flatMap { doc =>
      maybe(doc.split('\n').flatMap { line =>
        line.split('@') match {
          case Array(prefix, tag, rest @ _*) if tag.startsWith(fieldPrefix) =>
            Some((tag.substring(fieldPrefix.size) +: rest).mkString("@").trim)
          case _ => None
        }
      })
    }.map(_.mkString(" "))
  }

  private def toServers(serverUrls: Seq[String]): Option[Servers] =
    maybe(serverUrls.map(url => Server(url = url)).toList)

  private def toPaths(functions: Iterable[(RpcFunction, RpcSchema)]): Paths = functions.map { case (function, schema) =>
    val requestMediaType = MediaType(schema = Some(schema.request))
    val resultMediaType = MediaType(schema = Some(schema.result))
    val errorMediaType = MediaType(schema = Some(schema.error))
    val requestBody = RequestBody(content = Map(contentType -> requestMediaType), required = Some(true))
    val responses = Map(
      "default" -> Response("Failed function call error details", Some(Map(contentType -> errorMediaType))),
      httpStatusCodeOk -> Response("Succesful function call result value", Some(Map(contentType -> resultMediaType)))
    )
    val operation = Operation(requestBody = Some(requestBody), responses = Some(responses))
    val summary = function.documentation.flatMap(_.split('\n').find {
      case scaladocMarkup(_*) => true
      case _ => false
    }.map(_.trim))
    val description = function.documentation
    val path = s"/${function.name.replace('.', '/')}"
    val pathItem = PathItem(post = Some(operation), summary = summary, description = description)
    path -> pathItem
  }.toMap

  private def toComponents(): Option[Components] = None

  private def maybe[T <: Iterable[_]](iterable: T): Option[T] = if (iterable.isEmpty) None else Some(iterable)
}
