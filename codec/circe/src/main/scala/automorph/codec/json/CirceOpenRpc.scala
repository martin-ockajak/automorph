package automorph.codec.json

import automorph.description.OpenRpc
import automorph.description.openrpc._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject}

/** JSON-RPC protocol support for Circe message codec plugin using JSON format. */
private[automorph] object CirceOpenRpc {

  def openRpcEncoder: Encoder[OpenRpc] = {
    implicit val schemaEncoder: Encoder[Schema] = new Encoder[Schema] {

      override def apply(a: Schema): Json = {
        val fields = Seq(
          a.`type`.map(v => "type" -> Json.fromString(v)),
          a.title.map(v => "title" -> Json.fromString(v)),
          a.description.map(v => "description" -> Json.fromString(v)),
          a.properties.map(v => "properties" -> Json.obj(v.view.mapValues(apply).toSeq*)),
          a.required.map(v => "required" -> Json.arr(v.map(Json.fromString)*)),
          a.default.map(v => "default" -> Json.fromString(v)),
          a.allOf.map(v => "allOf" -> Json.arr(v.map(apply)*)),
          a.$ref.map(v => "$ref" -> Json.fromString(v))
        ).flatten
        Json.obj(fields*)
      }
    }
    implicit val contactEncoder: Encoder[Contact] = deriveEncoder[Contact]
    implicit val contentDescriptorEncoder: Encoder[ContentDescriptor] = deriveEncoder[ContentDescriptor]
    implicit val externalDocumentationEncoder: Encoder[ExternalDocumentation] = deriveEncoder[ExternalDocumentation]
    implicit val errorEncoder: Encoder[Error] = deriveEncoder[Error]
    implicit val exampleEncoder: Encoder[Example] = deriveEncoder[Example]
    implicit val licenseEncoder: Encoder[License] = deriveEncoder[License]
    implicit val serverVariableEncoder: Encoder[ServerVariable] = deriveEncoder[ServerVariable]
    implicit val examplePairingEncoder: Encoder[ExamplePairing] = deriveEncoder[ExamplePairing]
    implicit val infoEncoder: Encoder[Info] = deriveEncoder[Info]
    implicit val serverEncoder: Encoder[Server] = deriveEncoder[Server]
    implicit val tagEncoder: Encoder[Tag] = deriveEncoder[Tag]
    implicit val linkEncoder: Encoder[Link] = deriveEncoder[Link]
    implicit val componentsEncoder: Encoder[Components] = deriveEncoder[Components]
    implicit val methodEncoder: Encoder[Method] = deriveEncoder[Method]

    deriveEncoder[OpenRpc]
  }

  def openRpcDecoder: Decoder[OpenRpc] = {
    implicit val schemaDecoder: Decoder[Schema] = new Decoder[Schema] {

      private val propertiesField = "properties"
      private val allOfField = "allOf"

      override def apply(c: HCursor): Decoder.Result[Schema] =
        decode(c)

      private def decode(c: ACursor): Decoder.Result[Schema] = {
        c.keys.map(_.toSet).map { keys =>
          for {
            `type` <- field[String](c, keys, "type")
              title <- field[String](c, keys, "title")
              description <- field[String](c, keys, "description")
              properties <- Option.when(keys.contains(propertiesField)) {
                val jsonObject = c.downField(propertiesField)
                jsonObject.keys.getOrElse(Seq())
                  .foldLeft(Right(Map[String, Schema]()).withLeft[DecodingFailure]) { case (result, key) =>
                    result.flatMap { schemas =>
                      decode(jsonObject.downField(key)).map(schema => schemas + (key -> schema))
                    }
                  }.map(Some.apply)
              }.getOrElse(Right(None))
              required <- field[List[String]](c, keys, "required")
              default <- field[String](c, keys, "default")
              allOf <- Option.when(keys.contains(allOfField)) {
                val jsonArray = c.downField(allOfField)
                jsonArray.values.map(_.toSeq).getOrElse(Seq()).indices
                  .foldLeft(Right(List[Schema]()).withLeft[DecodingFailure]) { case (result, index) =>
                    result.flatMap { schemas =>
                      decode(jsonArray.downN(index)).map(schemas :+ _)
                    }
                  }.map(Some.apply)
              }.getOrElse(Right(None))
              $ref <- field[String](c, keys, "$ref")
          } yield {
            new Schema(
              `type` = `type`,
              title = title,
              description = description,
              properties = properties,
              required = required,
              default = default,
              allOf = allOf,
              $ref = $ref
            )
          }
        }.getOrElse(Left(DecodingFailure("Not a JSON object", c.history)))
      }

      private def field[T](c: ACursor, keys: Set[String], name: String)(implicit
        decoder: Decoder[Option[T]]
      ): Decoder.Result[Option[T]] = {
        if (keys.contains(name)) {
          c.downField(name).as[Option[T]]
        } else {
          Right(None)
        }
      }
    }
    implicit val contactDecoder: Decoder[Contact] = deriveDecoder[Contact]
    implicit val contentDescriptorDecoder: Decoder[ContentDescriptor] = deriveDecoder[ContentDescriptor]
    implicit val externalDocumentationDecoder: Decoder[ExternalDocumentation] = deriveDecoder[ExternalDocumentation]
    implicit val errorDecoder: Decoder[Error] = deriveDecoder[Error]
    implicit val exampleDecoder: Decoder[Example] = deriveDecoder[Example]
    implicit val licenseDecoder: Decoder[License] = deriveDecoder[License]
    implicit val serverVariableDecoder: Decoder[ServerVariable] = deriveDecoder[ServerVariable]
    implicit val examplePairingDecoder: Decoder[ExamplePairing] = deriveDecoder[ExamplePairing]
    implicit val infoDecoder: Decoder[Info] = deriveDecoder[Info]
    implicit val serverDecoder: Decoder[Server] = deriveDecoder[Server]
    implicit val tagDecoder: Decoder[Tag] = deriveDecoder[Tag]
    implicit val linkDecoder: Decoder[Link] = deriveDecoder[Link]
    implicit val componentsDecoder: Decoder[Components] = deriveDecoder[Components]
    implicit val methodDecoder: Decoder[Method] = deriveDecoder[Method]

    deriveDecoder[OpenRpc]
  }
}
