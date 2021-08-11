package automorph.openapi

import automorph.openapi.Schema.Properties
import automorph.openapi.Specification.{Components, Paths, Servers}
import automorph.spi.protocol.RpcFunction

/**
 * Open API specification generator.
 *
 * @see https://github.com/OAI/OpenAPI-Specification
 */
private[automorph] object OpenApi {

  val requestTitle = "Request"
  val resultTitle = "Result"
  val errorTitle = "Error"
  val objectType = "object"
  val argumentsDescription = "Function argument values by name"
  private val contentType = "application/json"
  private val httpStatusCodeOk = 200.toString
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
   * Generate OpenAPI specification for given RPC API functions.
   *
   * @param functionSchemas API functions with RPC schemas
   * @param title API title
   * @param version API specification version
   * @param serverUrls API server URLs
   * @return OpenAPI specification
   */
  def specification(
    functionSchemas: Iterable[(RpcFunction, RpcSchema)],
    title: String,
    version: String,
    serverUrls: Seq[String]
  ): Specification = Specification(
    info = Info(title = title, version = version),
    servers = servers(serverUrls),
    paths = Some(paths(functionSchemas)),
    components = components()
  )

  def parameterSchemas(function: RpcFunction): Map[String, Schema] =
    function.parameters.map { parameter =>
      // FIXME - convert data type to JSON type
      parameter.name -> Schema(
        Some(parameter.dataType),
        Some(parameter.name),
        scaladocField(function.documentation, s"param ${parameter.name}")
      )
    }.toMap

  def resultSchema(function: RpcFunction): Schema =
    // FIXME - convert data type to JSON type
    Schema(Some(function.resultType), Some("result"), scaladocField(function.documentation, "return"))

  def requiredParameters(function: RpcFunction): List[String] =
    function.parameters.filter(_.dataType.startsWith(optionTypePrefix)).map(_.name).toList

  def maybe[T <: Iterable[_]](iterable: T): Option[T] = Option.when(iterable.nonEmpty)(iterable)

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

  private def servers(serverUrls: Seq[String]): Option[Servers] =
    maybe(serverUrls.map(url => Server(url = url)).toList)

  private def paths(functionSchemas: Iterable[(RpcFunction, RpcSchema)]): Paths = functionSchemas.map { case (function, schema) =>
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

  private def components(): Option[Components] = None
}
