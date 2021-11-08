package automorph.codec.json

import automorph.description.{OpenApi, OpenRpc, openapi, openrpc}
import automorph.protocol.jsonrpc.{Message, MessageError}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json, JsonObject}

/**
 * JSON-RPC protocol support for Circe message codec plugin using JSON format.
 */
private[automorph] object CirceJsonRpc {
  type RpcMessage = Message[Json]

  lazy val messageEncoder: Encoder[Message[Json]] = {
    implicit val idEncoder: Encoder[Message.Id] = Encoder.encodeJson.contramap[Message.Id] {
      case Right(id) => Json.fromString(id)
      case Left(id) => Json.fromBigDecimal(id)
    }
    implicit val paramsEncoder: Encoder[Message.Params[Json]] =
      Encoder.encodeJson.contramap[Message.Params[Json]] {
        case Right(params) => Json.fromJsonObject(JsonObject.fromMap(params))
        case Left(params) => Json.fromValues(params)
      }
    implicit val messageErrorEncoder: Encoder[MessageError[Json]] = deriveEncoder[MessageError[Json]]

    deriveEncoder[Message[Json]]
  }

  lazy val messageDecoder: Decoder[Message[Json]] = {
    implicit val idDecoder: Decoder[Message.Id] = Decoder.decodeJson.map(_.fold(
      invalidId(None.orNull),
      invalidId,
      id => id.toBigDecimal.map(Left.apply).getOrElse(invalidId(id)),
      id => Right(id),
      invalidId,
      invalidId
    ))
    implicit val paramsDecoder: Decoder[Message.Params[Json]] = Decoder.decodeJson.map(_.fold(
      invalidParams(None.orNull),
      invalidParams,
      invalidParams,
      invalidParams,
      params => Left(params.toList),
      params => Right(params.toMap)
    ))
    implicit val messageErrorDecoder: Decoder[MessageError[Json]] = deriveDecoder[MessageError[Json]]

    deriveDecoder[Message[Json]]
  }

  implicit val schemaEncoder: Encoder[openrpc.Schema] = new Encoder[openrpc.Schema] {
    override def apply(a: openrpc.Schema): Json = {
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

  lazy val openRpcEncoder: Encoder[OpenRpc] = {
    implicit val contactEncoder: Encoder[openrpc.Contact] = deriveEncoder[openrpc.Contact]
    implicit val contentDescriptorEncoder: Encoder[openrpc.ContentDescriptor] = deriveEncoder[openrpc.ContentDescriptor]
    implicit val externalDocumentationEncoder: Encoder[openrpc.ExternalDocumentation] = deriveEncoder[openrpc.ExternalDocumentation]
    implicit val errorEncoder: Encoder[openrpc.Error] = deriveEncoder[openrpc.Error]
    implicit val exampleEncoder: Encoder[openrpc.Example] = deriveEncoder[openrpc.Example]
    implicit val licenseEncoder: Encoder[openrpc.License] = deriveEncoder[openrpc.License]
    implicit val serverVariableEncoder: Encoder[openrpc.ServerVariable] = deriveEncoder[openrpc.ServerVariable]
    implicit val examplePairingEncoder: Encoder[openrpc.ExamplePairing] = deriveEncoder[openrpc.ExamplePairing]
    implicit val infoEncoder: Encoder[openrpc.Info] = deriveEncoder[openrpc.Info]
    implicit val serverEncoder: Encoder[openrpc.Server] = deriveEncoder[openrpc.Server]
    implicit val tagEncoder: Encoder[openrpc.Tag] = deriveEncoder[openrpc.Tag]
    implicit val linkEncoder: Encoder[openrpc.Link] = deriveEncoder[openrpc.Link]
    implicit val componentsEncoder: Encoder[openrpc.Components] = deriveEncoder[openrpc.Components]
    implicit val methodEncoder: Encoder[openrpc.Method] = deriveEncoder[openrpc.Method]

    deriveEncoder[OpenRpc]
  }

  implicit val schemaDecoder: Decoder[openrpc.Schema] = new Decoder[openrpc.Schema] {
    private val propertiesField = "properties"
    private val allOfField = "allOf"

    override def apply(c: HCursor): Decoder.Result[openrpc.Schema] =
      decode(c)

    private def decode(c: ACursor): Decoder.Result[openrpc.Schema] = {
      c.keys.map(_.toSet).map { keys =>
        for {
          `type` <- field[String](c, keys, "type")
            title <- field[String](c, keys, "title")
            description <- field[String](c, keys, "description")
            properties <- Option.when(keys.contains(propertiesField)) {
              val jsonObject = c.downField(propertiesField)
              jsonObject.keys.getOrElse(Seq())
                .foldLeft(Right(Map[String, openrpc.Schema]()).withLeft[DecodingFailure]) { case (result, key) =>
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
                .foldLeft(Right(List[openrpc.Schema]()).withLeft[DecodingFailure]) { case (result, index) =>
                  result.flatMap { schemas =>
                    decode(jsonArray.downN(index)).map(schemas :+ _)
                  }
                }.map(Some.apply)
            }.getOrElse(Right(None))
            $ref <- field[String](c, keys, "$ref")
        } yield {
          new openrpc.Schema(
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

  lazy val openRpcDecoder: Decoder[OpenRpc] = {
    implicit val contactDecoder: Decoder[openrpc.Contact] = deriveDecoder[openrpc.Contact]
    implicit val contentDescriptorDecoder: Decoder[openrpc.ContentDescriptor] = deriveDecoder[openrpc.ContentDescriptor]
    implicit val externalDocumentationDecoder: Decoder[openrpc.ExternalDocumentation] = deriveDecoder[openrpc.ExternalDocumentation]
    implicit val errorDecoder: Decoder[openrpc.Error] = deriveDecoder[openrpc.Error]
    implicit val exampleDecoder: Decoder[openrpc.Example] = deriveDecoder[openrpc.Example]
    implicit val licenseDecoder: Decoder[openrpc.License] = deriveDecoder[openrpc.License]
    implicit val serverVariableDecoder: Decoder[openrpc.ServerVariable] = deriveDecoder[openrpc.ServerVariable]
    implicit val examplePairingDecoder: Decoder[openrpc.ExamplePairing] = deriveDecoder[openrpc.ExamplePairing]
    implicit val infoDecoder: Decoder[openrpc.Info] = deriveDecoder[openrpc.Info]
    implicit val serverDecoder: Decoder[openrpc.Server] = deriveDecoder[openrpc.Server]
    implicit val tagDecoder: Decoder[openrpc.Tag] = deriveDecoder[openrpc.Tag]
    implicit val linkDecoder: Decoder[openrpc.Link] = deriveDecoder[openrpc.Link]
    implicit val componentsDecoder: Decoder[openrpc.Components] = deriveDecoder[openrpc.Components]
    implicit val methodDecoder: Decoder[openrpc.Method] = deriveDecoder[openrpc.Method]

    deriveDecoder[OpenRpc]
  }

  private def invalidId(value: Any): Message.Id =
    throw new IllegalArgumentException(s"Invalid request identifier: $value")

  private def invalidParams(value: Any): Message.Params[Json] =
    throw new IllegalArgumentException(s"Invalid request parameters: $value")
}
